package com.ytchannelcli;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class SimpleDownloader extends Downloader {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(request.url()).openConnection();
            connection.setRequestMethod(request.httpMethod());
            
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

            for (Map.Entry<String, List<String>> header : request.headers().entrySet()) {
                for (String value : header.getValue()) {
                    connection.addRequestProperty(header.getKey(), value);
                }
            }

            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            InputStream inputStream = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();

            String body = null;
            if (inputStream != null) {
                try (java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A")) {
                    body = s.hasNext() ? s.next() : "";
                }
            }

            String finalUrl = connection.getURL().toString();

            return new Response(responseCode, responseMessage, connection.getHeaderFields(), body, finalUrl);
        } catch (Exception e) {
            if (e instanceof ReCaptchaException) {
                throw (ReCaptchaException) e;
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new RuntimeException("Failed to download content from " + request.url(), e);
        }
    }
}
