import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class SonarReports {
    public static String COOKIE = null;
    public static void main(String[] args) {
        System.out.println("Running main class");
        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
        try {
            File authCookieFile = new File("authCookie.txt");
            COOKIE = FileUtils.readFileToString(authCookieFile);

            File projectKeysFiles = new File("project-keys.txt");
            List<String> projectKeys = FileUtils.readLines(projectKeysFiles);

            File outputFile = new File("sonar-reports-" + new Date().getTime());
            projectKeys.forEach(projectKey -> {
                try {
                    String outputJson = getSonarReport(projectKey);
                    if(outputJson != null){
                        FileUtils.writeStringToFile(outputFile, outputJson);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSonarReport(String projectKey) {
        URI uri = null;
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+SSSz");
            uri = new URIBuilder("http://sonar.kroger.com/sonar/api/measures/search_history")
                    .addParameter("from", LocalDateTime.now().format(dateTimeFormatter))
                    .addParameter("component", projectKey)
                    .addParameter("metrics", "bugs,vulnerabilities,sqale_index,duplicated_lines_density,ncloc,coverage,lines_to_cover,uncovered_lines")
                    .addParameter("ps", "1000")
                    .build();
            HttpGet request = new HttpGet(uri);
            request.setHeader("Cookie", COOKIE);
            CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
            CloseableHttpResponse response = httpClient.execute(request);
            System.out.println("Status code:" + response.getStatusLine().getStatusCode());
            HttpEntity httpEntity = response.getEntity();
            if(httpEntity != null){
                return EntityUtils.toString(httpEntity);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
