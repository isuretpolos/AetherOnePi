package de.isuret.polos.AetherOnePi.processing.communication;

import de.isuret.polos.AetherOnePi.domain.AetherOnePiStatus;

public interface IStatusReceiver {

    void receivingStatus(AetherOnePiStatus status);
    void setHotbitsPercentage(Float percentage);
}
