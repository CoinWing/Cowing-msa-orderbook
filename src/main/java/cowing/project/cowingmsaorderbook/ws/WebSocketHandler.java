package cowing.project.cowingmsaorderbook.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import cowing.project.cowingmsaorderbook.dto.OrderbookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends BinaryWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final OrderbookService orderbookService;

    // 구독할 코인 목록 (기존과 동일)
    private static final List<String> COIN_CODES = List.of("KRW-XRP", "KRW-ETH", "KRW-BTC", "KRW-SNT", "KRW-ALT", "KRW-USDT", "KRW-SUI", "KRW-SOL",
            "KRW-DOGE", "KRW-POKT", "KRW-BORA", "KRW-RVN", "KRW-ORBS", "KRW-UNI", "KRW-ONDO", "KRW-VIRTUAL", "KRW-SOPH", "KRW-ADA", "KRW-NXPC", "KRW-ANIME",
            "KRW-PEPE", "KRW-TRUMP", "KRW-AGLD", "KRW-BCH", "KRW-ENS", "KRW-SEI", "KRW-SHIB", "KRW-STMX", "KRW-AAVE", "KRW-WCT", "KRW-STRAX", "KRW-LAYER",
            "KRW-TFUEL", "KRW-LINK", "KRW-MOVE", "KRW-TRX", "KRW-KAITO", "KRW-NEAR", "KRW-ARB", "KRW-STX", "KRW-HBAR", "KRW-XLM", "KRW-UXLINK", "KRW-ZRO", "KRW-AVAX", "KRW-SAND", "KRW-MASK", "KRW-T",
            "KRW-DOT", "KRW-POL", "KRW-AXL", "KRW-ME", "KRW-MEW", "KRW-ETC", "KRW-VANA", "KRW-LPT", "KRW-JTO", "KRW-ALGO", "KRW-BONK", "KRW-DRIFT", "KRW-SONIC", "KRW-PYTH",
            "KRW-BERA", "KRW-A", "KRW-TAIKO", "KRW-BSV", "KRW-BTT", "KRW-BLUR", "KRW-AERGO", "KRW-IMX", "KRW-PENDLE", "KRW-ICX", "KRW-PENGU", "KRW-OM", "KRW-GRT",
            "KRW-COMP", "KRW-ATH", "KRW-XEM", "KRW-NEO", "KRW-INJ", "KRW-JUP", "KRW-BIGTIME", "KRW-APT", "KRW-BEAM", "KRW-SIGN", "KRW-ZETA", "KRW-AKT", "KRW-CRO", "KRW-ARDR", "KRW-VET", "KRW-GAS",
            "KRW-W", "KRW-ATOM", "KRW-GMT", "KRW-AXS", "KRW-TT", "KRW-CTC", "KRW-FLOW", "KRW-AUCTION", "KRW-CARV", "KRW-MOCA", "KRW-MLK", "KRW-MANA", "KRW-PUNDIX",
            "KRW-FIL", "KRW-IOST", "KRW-WAVES", "KRW-DEEP", "KRW-MNT", "KRW-XEC", "KRW-RENDER", "KRW-CHZ", "KRW-SXP", "KRW-ORCA", "KRW-QTUM", "KRW-IOTA", "KRW-HIVE", "KRW-CVC",
            "KRW-TOKAMAK", "KRW-BLAST", "KRW-THETA", "KRW-HUNT", "KRW-MINA", "KRW-G", "KRW-POLYX", "KRW-SAFE", "KRW-TIA", "KRW-ARKM", "KRW-AWE", "KRW-WAXP", "KRW-STRIKE", "KRW-CELO", "KRW-ARK",
            "KRW-ID", "KRW-ZRX", "KRW-HP", "KRW-ZIL", "KRW-GLM", "KRW-GAME2", "KRW-CKB", "KRW-VTHO", "KRW-STG", "KRW-COW", "KRW-USDC", "KRW-EGLD", "KRW-MED", "KRW-KAVA", "KRW-XTZ",
            "KRW-ASTR", "KRW-AQT", "KRW-IQ", "KRW-ONT", "KRW-WAL", "KRW-MVL", "KRW-ELF", "KRW-LSK", "KRW-ONG", "KRW-DKA", "KRW-ANKR", "KRW-MTL", "KRW-JST", "KRW-MBL", "KRW-BOUNTY",
            "KRW-SC", "KRW-QKC", "KRW-1INCH", "KRW-STEEM", "KRW-STORJ", "KRW-MOC", "KRW-META", "KRW-BAT", "KRW-POWR", "KRW-AHT", "KRW-KNC", "KRW-CBK", "KRW-GRS", "KRW-FCT2");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Upbit WebSocket connected: {}", session.getId());

        // 구독 메시지 전송
        sendSubscriptionMessage(session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // ByteBuffer를 String으로 변환
        ByteBuffer payload = message.getPayload();
        String jsonPayload = StandardCharsets.UTF_8.decode(payload).toString();
        log.debug("Received message: {}", jsonPayload);

        try {
            OrderbookDto orderbookDto = objectMapper.readValue(jsonPayload, OrderbookDto.class);

            // Redis 저장
            orderbookService.updateOrderbook(orderbookDto);
        } catch (Exception e) {
            log.error("Error processing message: {}", jsonPayload, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Upbit WebSocket disconnected: {} with status {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error on session {}: {}", session.getId(), exception.getMessage());
    }

    // 구독 메시지 전송 로직은 TextMessage를 사용하는 것이 맞으므로 변경 없음
    private void sendSubscriptionMessage(WebSocketSession session) throws IOException {
        String ticket = UUID.randomUUID().toString();

        String subscriptionMessage = String.format(
                "[{\"ticket\":\"%s\"},{\"type\":\"orderbook\",\"codes\":[%s]}]",
                ticket,
                COIN_CODES.stream()
                        .map(code -> "\"" + code + "\"")
                        .collect(Collectors.joining(","))
        );

        session.sendMessage(new TextMessage(subscriptionMessage));
        log.info("Sent subscription message for {} coins", COIN_CODES.size());
    }
}