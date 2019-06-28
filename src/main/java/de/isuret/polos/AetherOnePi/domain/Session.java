package de.isuret.polos.AetherOnePi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {

    private String intention;
    private String description;
    private Calendar created;
    private List<AnalysisResult> analysisResults = new ArrayList<>();
    private List<BroadCastData> broadCastedList = new ArrayList<>();
}
