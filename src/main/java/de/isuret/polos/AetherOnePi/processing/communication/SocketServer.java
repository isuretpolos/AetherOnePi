package de.isuret.polos.AetherOnePi.processing.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A socket server listening to status messages deriving from the AetherOnePi server in the Raspberry Pi
 */
public class SocketServer {

    private ServerSocket serverSocket;

    public void start(int port, IStatusReceiver statusReceiver) throws IOException {
        serverSocket = new ServerSocket(port);
        while (true)
            new AetherOnePiClientHandler(serverSocket.accept(), statusReceiver).start();
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    private static class AetherOnePiClientHandler extends Thread {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private IStatusReceiver statusReceiver;

        public AetherOnePiClientHandler(Socket socket, IStatusReceiver statusReceiver) {

            this.clientSocket = socket;
            this.statusReceiver = statusReceiver;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    try {

                        ObjectMapper mapper = new ObjectMapper();
                        AetherOnePiStatus status = mapper.readValue(inputLine, AetherOnePiStatus.class);
                        statusReceiver.receivingStatus(status);
                        inputLine = mapper.writeValueAsString(status);

                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                    out.println(inputLine);
                }

                in.close();
                out.close();
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
