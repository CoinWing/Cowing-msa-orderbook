package cowing.project.cowingmsaorderbook.ws;

import cowing.project.cowingmsaorderbook.dto.OrderbookDto;
import cowing.project.cowingmsaorderbook.dto.OrderbookUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderbookService {

    private final RedisTemplate<String, String> redisTemplate;

    public void updateOrderbook(OrderbookDto orderbookDto) {
        String code = orderbookDto.code();
        String asksKey = "orderbook:" + code + ":asks";
        String bidsKey = "orderbook:" + code + ":bids";
        String metaKey = "orderbook:" + code + ":meta";

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

        // 2. 파이프라이닝 사용: 트랜잭션 오버헤드 제거, 네트워크 라운드트립 최소화
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 기존 데이터 삭제
            connection.del(asksKey.getBytes());
            connection.del(bidsKey.getBytes());
            connection.del(metaKey.getBytes());
            
            // 3. 배치 처리: 한 번에 모든 데이터 처리
            if (!askTuples.isEmpty()) {
                redisTemplate.opsForZSet().add(asksKey, askTuples);
            }
            if (!bidTuples.isEmpty()) {
                redisTemplate.opsForZSet().add(bidsKey, bidTuples);
            }
            
            // 메타데이터 배치 업데이트
            if (!metaData.isEmpty()) {
                redisTemplate.opsForHash().putAll(metaKey, metaData);
            }
            
            return null; // 파이프라인에서는 null 반환
        });
    }
}