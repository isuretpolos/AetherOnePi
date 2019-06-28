package de.isuret.polos.AetherOnePi.hotbits;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotBitIntegers {

    private List<Integer> integerList = new ArrayList<>();
}
