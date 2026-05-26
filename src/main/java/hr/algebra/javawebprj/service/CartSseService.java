package hr.algebra.javawebprj.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class CartSseService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersBySession = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String sessionId, int initialCount) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersBySession.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(e -> removeEmitter(sessionId, emitter));

        sendCount(emitter, initialCount);
        return emitter;
    }

    public void publishCount(String sessionId, int count) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersBySession.get(sessionId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendCount(emitter, count);
        }
    }

    private void sendCount(SseEmitter emitter, int count) {
        try {
            emitter.send(SseEmitter.event().name("cart-count").data(count));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emittersBySession.get(sessionId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersBySession.remove(sessionId);
            }
        }
    }
}
