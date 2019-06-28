package de.isuret.polos;

import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;
import de.isuret.polos.AetherOnePi.processing.communication.SocketClient;
import de.isuret.polos.AetherOnePi.processing.communication.SocketServer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class SocketServerTest {

    private SocketServer socketServer;


//    @Before
//    public void setup() {
//
//        (new Thread() {
//            public void run() {
//                try {
//
//                    socketServer = new SocketServer();
//
//                    try {
//                        socketServer.start(5555, null);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
//
//    @Test
//    public void test() throws IOException {
//
//        SocketClient client1 = new SocketClient();
//        client1.startConnection("127.0.0.1", 5555);
//
//        AetherOnePiStatus status = new AetherOnePiStatus();
//        status.setHotbitsPackages(80);
//        status = client1.sendStatus(status);
//
//        System.out.println(status);
//
//    }

}
