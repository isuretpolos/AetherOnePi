package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BroadCastData {

    private Boolean clear = false;
    private String intention;
    private String signature;
    private Integer delay = 25;
    private Integer repeat = 1;
    private Integer enteringWithGeneralVitality;
    private Integer leavingWithGeneralVitality;
}
