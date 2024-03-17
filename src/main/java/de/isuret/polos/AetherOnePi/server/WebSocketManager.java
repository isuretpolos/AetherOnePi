package de.isuret.polos.AetherOnePi.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static void addSession(String id, Session session) {
        sessions.put(id, session);
    }

    public static void removeSession(String id) {
        sessions.remove(id);
    }

    public static void updateAnalysis() throws JsonProcessingException {

        if (sessions.isEmpty()) return;

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(AetherOneUI.p.getAnalysisResult());

        for (Session session : sessions.values()) {
            if (session != null && session.isOpen()) {
                try {
                    session.getRemote().sendString(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
