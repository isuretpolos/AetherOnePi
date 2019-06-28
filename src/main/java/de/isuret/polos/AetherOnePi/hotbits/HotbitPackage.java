package de.isuret.polos.AetherOnePi.hotbits;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotbitPackage {

    private String fileName;
    private String hotbits;
    private Integer originalSize;
}
