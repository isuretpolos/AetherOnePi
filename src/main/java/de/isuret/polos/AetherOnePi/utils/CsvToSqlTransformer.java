package de.isuret.polos.AetherOnePi.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CsvToSqlTransformer {

    public static void main(String[] args) throws IOException {

        generateSql("kentRemedies","V1_2__homeopathy_rates_kent", "Kent Remedies","Homeopathy");
        generateSql("JanScholtenWonderfulPlants_List","V1_3__homeopathy_rates_JanScholten", "Wonderful Plants","Homeopathy");
        return;
    }

    private static void generateSql(String sourceFileName, String sqlFileName, String sourceName, String groupName) throws IOException {

        List<String> lines = FileUtils.readLines(new File("src/main/resources/rates/" + sourceFileName + ".txt"), "UTF-8");
        StringBuilder sqlContent = new StringBuilder();

        for (String line : lines) {

            StringBuilder sql = new StringBuilder().append("INSERT INTO RATE (SOURCE_NAME,GROUP_NAME,NAME) VALUES('");

            sql.append(sourceName).append("','");
            sql.append(groupName).append("','");
            sql.append(line.replaceAll("'", "").trim()).append("');\n");

            sqlContent.append(sql);
        }

        FileUtils.writeStringToFile(new File("src/main/resources/db/migration/" + sqlFileName + ".sql"), sqlContent.toString(), "UTF-8");
    }
}
