package de.isuret.polos.AetherOnePi.processing.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;
import lombok.Getter;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * The Server use this client to notify the Processing application about specific events taking place or progress of a specific task running.
 */
public class SocketClient {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    @Getter
    private String address;

    @PostConstruct
    public void init() {
    }

    public void startConnection(String ip, int port) throws IOException {
        address = ip;
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AetherOnePiStatus sendStatus(AetherOnePiStatus status) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        String statusJson = mapper.writeValueAsString(status);

        out.println(statusJson);
        String resp = in.readLine();

        return mapper.readValue(resp, AetherOnePiStatus.class);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }
}
