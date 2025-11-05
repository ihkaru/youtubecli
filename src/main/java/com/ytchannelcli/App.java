package com.ytchannelcli;

import java.io.IOException;
import java.util.Map;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import com.ytchannelcli.commands.GetDetails;
import com.ytchannelcli.commands.GetVideos;
import com.ytchannelcli.commands.Search;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "yt-cli", mixinStandardHelpOptions = true, version = "yt-cli 1.0.0", description = "A CLI tool to extract data from YouTube.", subcommands = {
        GetVideos.class,
        GetDetails.class,
        Search.class
})
public class App {

    static class SimpleDownloader extends Downloader {
        private final OkHttpClient client = new OkHttpClient.Builder().build();

        @Override
        public Response execute(Request request) throws IOException, ReCaptchaException {
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                    .url(request.url());

            // Handle request body for POST/PUT requests
            RequestBody body = null;
            if (request.dataToSend() != null && request.dataToSend().length > 0) {
                String contentType = "application/x-www-form-urlencoded";
                Map<String, java.util.List<String>> headers = request.headers();
                if (headers != null && headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                }
                body = RequestBody.create(
                        request.dataToSend(),
                        MediaType.parse(contentType));
            }

            builder.method(request.httpMethod(), body);

            // Add headers
            Map<String, java.util.List<String>> headers = request.headers();
            if (headers != null) {
                headers.forEach((key, values) -> values.forEach(value -> builder.addHeader(key, value)));
            }

            okhttp3.Response response = client.newCall(builder.build()).execute();
            ResponseBody responseBody = response.body();
            String responseBodyString = responseBody != null ? responseBody.string() : "";

            return new Response(
                    response.code(),
                    response.message(),
                    response.headers().toMultimap(),
                    responseBodyString,
                    request.url());
        }
    }

    public static void main(String[] args) {
        try {
            NewPipe.init(new SimpleDownloader());
        } catch (Exception e) {
            System.err.println(
                    String.format("{\"error\": \"Initialization failed\", \"message\": \"%s\"}", e.getMessage()));
            System.exit(1);
        }
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}