package cowing.project.cowingmsaorderbook.ws;

import cowing.project.cowingmsaorderbook.dto.OrderbookDto;
import cowing.project.cowingmsaorderbook.dto.OrderbookUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderbookService {

    private final RedisTemplate<String, String> redisTemplate;

    public void updateOrderbook(OrderbookDto orderbookDto) {
        String code = orderbookDto.code();
        String asksKey = "orderbook:" + code + ":asks";
        String bidsKey = "orderbook:" + code + ":bids";
        String metaKey = "orderbook:" + code + ":meta";

        String tempSuffix = String.valueOf(System.nanoTime());
        String tempAsksKey = asksKey + ":" + tempSuffix;
        String tempBidsKey = bidsKey + ":" + tempSuffix;
        String tempMetaKey = metaKey + ":" + tempSuffix;

        // 1. 사전 데이터 준비: 루프 내에서 Redis 호출 제거
        Set<ZSetOperations.TypedTuple<String>> askTuples = new HashSet<>();
        Set<ZSetOperations.TypedTuple<String>> bidTuples = new HashSet<>();

        // 배치 처리를 위한 데이터 준비
        for (OrderbookUnit unit : orderbookDto.orderbookUnits()) {
            askTuples.add(new DefaultTypedTuple<>(
                    unit.askSize().toPlainString(),
                    unit.askPrice().doubleValue()
            ));
            bidTuples.add(new DefaultTypedTuple<>(
                    unit.bidSize().toPlainString(),
                    unit.bidPrice().doubleValue()
            ));
        }

        // 메타데이터 준비
        Map<String, String> metaData = new HashMap<>();
        metaData.put("timestamp", String.valueOf(orderbookDto.timestamp()));
        metaData.put("total_ask_size", orderbookDto.totalAskSize().toPlainString());
        metaData.put("total_bid_size", orderbookDto.totalBidSize().toPlainString());

        // 2. 원자적 트랜잭션, 네트워크 라운드트립 최소화
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi(); // 트랜잭션 시작

                // 모든 데이터를 임시키에 저장
                if (!askTuples.isEmpty()) {
                    operations.opsForZSet().add(tempAsksKey, askTuples);
                }
                if (!bidTuples.isEmpty()) {
                    operations.opsForZSet().add(tempBidsKey, bidTuples);
                }
                operations.opsForHash().putAll(tempMetaKey, metaData);

                // 원자적 변경 (덮어쓰기)
                operations.rename(tempAsksKey, asksKey);
                operations.rename(tempBidsKey, bidsKey);
                operations.rename(tempMetaKey, metaKey);

                return operations.exec(); // 트랜잭션 실행
            }
        });
    }
}