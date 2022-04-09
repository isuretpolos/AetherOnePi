package de.isuret.polos.AetherOnePi.domain;

import java.util.Calendar;

/**
 * One session has a new meaning:
 * It consists of one action regarding a combination of analysis or / and broadcasting.
 * A new analysis is considered a new session.
 * A new broadcasting is also a new session.
 * For each of these actions one can note an intention or title and a description.
 * It is also possible to note only one description without any action.
 * A session object has only a created calender object and cannot be modified.
 */
public class Session {

    private String intention;
    private String description;
    private Calendar created;
    AnalysisResult analysisResult;
    BroadCastData broadCasted;

    /**
     * Makes a copy of the session (used during GV checks)
     * @param vo
     */
    public Session(Session vo) {
        intention = vo.intention;
        description = vo.description;
        created = Calendar.getInstance();
    }

    public Session() {}

    public String getIntention() {
        return intention;
    }

    public void setIntention(String intention) {
        this.intention = intention;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public void setAnalysisResult(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }

    public BroadCastData getBroadCasted() {
        return broadCasted;
    }

    public void setBroadCasted(BroadCastData broadCasted) {
        this.broadCasted = broadCasted;
    }
}
