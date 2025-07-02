package cowing.project.cowingmsaorderbook.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRunner {

    private final WebSocketHandler upbitWebSocketHandler;

    private static final String UPBIT_WEBSOCKET_URL = "wss://api.upbit.com/websocket/v1";
    private static final int MAX_RETRY_ATTEMPTS = 5; // 최대 5번
    private static final long INITIAL_RETRY_DELAY = 1000; // 1초
    private static final long MAX_RETRY_DELAY = 30000; // 30초


    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        connectWithRetry(0);
    }

    private void connectWithRetry(int attemptCount) {
        log.info("Upbit WebSocket 연결중... (시도 횟수: {})", attemptCount + 1);

        try {
            WebSocketClient client = new StandardWebSocketClient();
            client.execute(upbitWebSocketHandler, UPBIT_WEBSOCKET_URL);
            log.info("WebSocket 연결 성공");
        } catch (Exception e) {
            log.error("Upbit WebSocket 연결 실패 (시도 횟수: {}): {}",
                    attemptCount + 1, e.getMessage());

            if (attemptCount < MAX_RETRY_ATTEMPTS - 1) {
                long delay = Math.min(INITIAL_RETRY_DELAY * (1L << attemptCount), MAX_RETRY_DELAY);
                log.info("{}초 안에 연결 재시도...", delay);

                scheduleRetry(attemptCount + 1, delay);
            } else {
                log.error("최대 연결 시도 횟수 ({}) 에 도달했습니다. 연결 시도를 종료합니다.", MAX_RETRY_ATTEMPTS);
            }
        }
    }

    private void scheduleRetry(int nextAttempt, long delay) {
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                .execute(() -> connectWithRetry(nextAttempt));
    }
}