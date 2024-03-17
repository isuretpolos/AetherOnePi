package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.service.DataService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

public class CaseToHtml {

    public static final String UTF_8 = "UTF-8";

    private CaseToHtml() {
    }

    public static void main(String [] args) {
        DataService dataService = new DataService();

        try {
            Case caseObject = dataService.loadCase(new File(args[0]));
            CaseToHtml.transformCaseObjectIntoHtml(caseObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void transformCaseObjectIntoHtml(Case caseObject) {

        try {
            File htmlFile = new File("cases/" + caseObject.getName().replaceAll(" ", "") + ".html");

            StringBuilder html = new StringBuilder();
            String htmlString = "";

            try {
                htmlString = FileUtils.readFileToString(new File("src/main/resources/templates/caseHeader.html"), UTF_8).replaceAll("#TITLE#", caseObject.getName());
            }catch (Exception e){
                htmlString = IOUtils.toString(CaseToHtml.class.getClassLoader().getResourceAsStream("templates/caseHeader.html"));
            }

            html.append(htmlString);

            for (Session session : caseObject.getSessionList()) {

                writeSession(html, session);
            }

            try {
                htmlString = FileUtils.readFileToString(new File("src/main/resources/templates/caseFooter.html"), UTF_8).replaceAll("#TITLE#", caseObject.getName());
            }catch (Exception e){
                htmlString = IOUtils.toString(CaseToHtml.class.getClassLoader().getResourceAsStream("templates/caseFooter.html"));
            }

            html.append(htmlString);

            FileUtils.writeStringToFile(htmlFile, html.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeSession(StringBuilder html, Session session) {
        html.append("<div class=\"jumbotron\">");

        if (session.getIntention() != null) {
            html.append("<h2 class=\"display-4\">" + session.getIntention() + "</h2>");
        }

        if (session.getDescription() != null) {
            html.append("<p class=\"lead\">" + session.getDescription() + "</p>");
            html.append("<hr class=\"my-4\">");
        }

        writeAnalysisResult(html, session.getAnalysisResult());

        if (session.getBroadCasted() != null) {
            html.append("<hr class=\"my-4\">");
            html.append("<h4 class=\"display-5\">Broadcasts</h4>");
            writeBroadCastData(html, session.getBroadCasted());
        }

        html.append("</div>");
    }

    private static void writeBroadCastData(StringBuilder html, BroadCastData broadCastData) {
        html.append("<div class=\"alert alert-success\" role=\"alert\">\n" +
                broadCastData.getSignature() +
                "</div>");

    }

    private static void writeAnalysisResult(StringBuilder html, AnalysisResult analysisResult) {

        if (analysisResult == null) return;

        if (analysisResult.getRateObjects().size() == 0) return;

        html.append("<table class=\"table table-sm table-hover table-dark\">\n" +
                "  <thead>\n" +
                "    <tr>\n" +
                "      <th scope=\"col\">NO</th>\n" +
                "      <th scope=\"col\">EV</th>\n" +
                "      <th scope=\"col\">RATE / SIGNATURE</th>\n" +
                "      <th scope=\"col\">GV</th>\n" +
                "      <th scope=\"col\">GV RE</th>\n" +
                "      <th scope=\"col\">REC</th>\n" +
                "    </tr>\n" +
                "  </thead>\n" +
                "  <tbody>");

        int y = 1;

        for (RateObject rateObject : analysisResult.getRateObjects()) {

            String gv = "";
            String rec_gv = "";
            String rec = "";

            if (rateObject.getGv() != 0) gv = String.valueOf(rateObject.getGv());
            if (rateObject.getRecurringGeneralVitality() > 1) rec_gv = String.valueOf(rateObject.getRecurringGeneralVitality());
            if (rateObject.getRecurring() > 0) rec = String.valueOf(rateObject.getRecurring());

            String clazz = "";

            if (rateObject.getGv() > 1000) {
                clazz = " class=\"bg-success\"";
            }

            if (rateObject.getGv() <= 1000 && rateObject.getRecurring() > 0) {
                clazz = " class=\"bg-info\"";
            }

            html.append("<tr" + clazz + ">\n" +
                    "      <td>" + y + "</td>\n" +
                    "      <td>" + rateObject.getEnergeticValue() + "</td>\n" +
                    "      <td>" + rateObject.getNameOrRate() + "</td>\n" +
                    "      <td>" + gv + "</td>\n" +
                    "      <td>" + rec_gv + "</td>\n" +
                    "      <td>" + rec + "</td>\n" +
                    "    </tr>");

            y++;
        }

        html.append("</tbody>\n</table>\n");
    }
}
