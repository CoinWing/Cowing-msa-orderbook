package cowing.project.cowingmsaorderbook.ws;


import cowing.project.cowingmsaorderbook.dto.OrderbookDto;
import cowing.project.cowingmsaorderbook.dto.OrderbookUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderbookService {

    private final RedisTemplate<String, String> redisTemplate;

    public void updateOrderbook(OrderbookDto orderbookDto) {
        String code = orderbookDto.code();
        String asksKey = "orderbook:" + code + ":asks";
        String bidsKey = "orderbook:" + code + ":bids";
        String metaKey = "orderbook:" + code + ":meta";

        // SessionCallback을 사용하여 트랜잭션 실행
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) throws DataAccessException {
                // MULTI: 트랜잭션 시작 (Spring이 자동으로 호출)
                operations.multi();

                // 1. 기존 데이터 삭제
                operations.delete((K) asksKey);
                operations.delete((K) bidsKey);
                operations.delete((K) metaKey);

                // 2. 새 호가 데이터 추가 (ZADD)
                for (OrderbookUnit unit : orderbookDto.orderbookUnits()) {
                    operations.opsForZSet().add((K) asksKey, (V) unit.askSize().toPlainString(), unit.askPrice().doubleValue());
                    operations.opsForZSet().add((K) bidsKey, (V) unit.bidSize().toPlainString(), unit.bidPrice().doubleValue());
                }

                // 3. 메타데이터 업데이트 (HSET)
                operations.opsForHash().put((K) metaKey, "timestamp", String.valueOf(orderbookDto.timestamp()));
                operations.opsForHash().put((K) metaKey, "total_ask_size", orderbookDto.totalAskSize().toPlainString());
                operations.opsForHash().put((K) metaKey, "total_bid_size", orderbookDto.totalBidSize().toPlainString());

                // EXEC: 트랜잭션 내의 모든 명령 실행 (Spring이 자동으로 호출)
                // execute() 메서드가 반환되면 EXEC가 실행됩니다.
                return operations.exec();
            }
        });

        // txResults에는 각 명령어의 실행 결과가 담겨 옵니다 (성공 시 [true, true, ...])
        // 필요하다면 결과 검증 로직을 추가할 수 있습니다.
    }
}