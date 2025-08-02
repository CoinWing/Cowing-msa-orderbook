package cowing.project.cowingmsaorderbook.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import cowing.project.cowingmsaorderbook.dto.OrderbookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends BinaryWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final OrderbookService orderbookService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Broadcaster WebSocket connected: {}", session.getId());
        // Broadcaster가 이미 모든 데이터를 구독하고 있으므로 별도의 구독 메시지는 불필요
        log.info("Connected to broadcaster, ready to receive orderbook data");
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // ByteBuffer를 String으로 변환
        ByteBuffer payload = message.getPayload();
        String jsonPayload = StandardCharsets.UTF_8.decode(payload).toString();
        log.debug("Received message: {}", jsonPayload);

        processMessage(jsonPayload);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Text 메시지도 처리할 수 있도록 추가
        String jsonPayload = message.getPayload();
        log.debug("Received text message: {}", jsonPayload);

        processMessage(jsonPayload);
    }

    private void processMessage(String jsonPayload) {
        try {
            // 먼저 메시지 타입을 확인
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(jsonPayload);
            
            // type 필드가 orderbook인 경우만 처리
            if (jsonNode.has("type") && "orderbook".equals(jsonNode.get("type").asText())) {
                OrderbookDto orderbookDto = objectMapper.readValue(jsonPayload, OrderbookDto.class);
                
                // Redis 저장 - 별도 예외 처리로 Redis 실패 시에도 메시지 처리 계속
                try {
                    orderbookService.updateOrderbook(orderbookDto);
                    log.debug("Processed orderbook data for: {}", orderbookDto.code());
                } catch (RedisConnectionFailureException e) {
                    log.warn("Redis connection failed for orderbook {}: {}. Message processing continues.", 
                             orderbookDto.code(), e.getMessage());
                } catch (DataAccessException e) {
                    log.warn("Redis data access error for orderbook {}: {}. Message processing continues.", 
                             orderbookDto.code(), e.getMessage());
                } catch (Exception e) {
                    log.error("Unexpected error saving orderbook {} to Redis: {}. Message processing continues.", 
                              orderbookDto.code(), e.getMessage());
                }
            } else {
                // orderbook이 아닌 데이터는 무시 (ticker 등)
                log.debug("Ignoring non-orderbook message");
            }
        } catch (Exception e) {
            // JSON 파싱이나 DTO 변환 실패 시에만 전체 메시지 처리 실패로 간주
            log.error("Error parsing message: {}", jsonPayload, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Broadcaster WebSocket disconnected: {} with status {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error on session {}: {}", session.getId(), exception.getMessage());
    }
}