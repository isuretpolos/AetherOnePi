package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.BroadCastData;
import de.isuret.polos.AetherOnePi.processing.communication.StatusNotificationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple queue, broadcasting one rate after another as long one is inside the list
 */
@Service
public class BroadcastQueue {

    private Log logger = LogFactory.getLog(BroadcastQueue.class);

    private List<BroadCastData> broadCastDataList = new ArrayList<>();

    @Autowired
    private BroadCastService broadCastService;

    @Autowired
    private StatusNotificationService statusNotificationService;

    @PostConstruct
    public void init() {
        queueProcessing();
    }

    public BroadCastData addBroadcastDataToQueue(BroadCastData data) {
        logger.info("addBroadcastDataToQueue " + data.getSignature());
        broadCastDataList.add(data);
        return data;
    }

    public void queueProcessing() {

        logger.info("Initializing queue processing");
        (new Thread() {
            public void run() {
                while (true) {

                    statusNotificationService.getStatus().setQueue(broadCastDataList.size());

                    if (!broadCastService.getBroadcasting() && broadCastDataList.size() > 0) {
                        broadcastFromQueue();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void broadcastFromQueue() {
        logger.info("broadcasting from queue ...");

        if (broadCastDataList.size() > 0) {
            broadCastService.broadcast(broadCastDataList.remove(0));
        }
    }
}
