package com.ytchannelcli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONObject;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-details", mixinStandardHelpOptions = true, description = "Fetches details for a single YouTube video.")
public class GetDetails implements Callable<Integer> {

    @Option(names = { "--url" }, required = true, description = "The URL of the YouTube video.")
    private String videoUrl;

    @Option(names = { "--transcript" }, description = "Fetch the transcript/subtitles for the video.")
    private boolean fetchTranscript;

    @Override
    public Integer call() {
        try {
            StreamExtractor extractor = NewPipe.getService(0)
                    .getStreamExtractor(videoUrl);
            extractor.fetchPage();

            JSONObject details = new JSONObject();
            details.put("title", extractor.getName());

            try {
                details.put("description", extractor.getDescription().getContent());
            } catch (Exception e) {
                details.put("description", "");
            }

            try {
                details.put("duration_seconds", extractor.getLength());
            } catch (Exception e) {
                // Duration not available
            }

            try {
                details.put("view_count", extractor.getViewCount());
            } catch (Exception e) {
                // View count not available
            }

            try {
                details.put("upload_date", extractor.getUploadDate().offsetDateTime().toString());
            } catch (Exception e) {
                // Upload date not available
            }

            if (fetchTranscript) {
                try {
                    List<SubtitlesStream> subtitles = extractor.getSubtitles(null);
                    if (subtitles != null && !subtitles.isEmpty()) {
                        SubtitlesStream firstSubtitle = subtitles.get(0);
                        String transcriptUrl = firstSubtitle.getContent();
                        org.schabi.newpipe.extractor.downloader.Response response = NewPipe.getDownloader()
                                .get(transcriptUrl);
                        details.put("transcript", response.responseBody());
                    }
                } catch (Exception e) {
                    details.put("transcript_error", e.getMessage());
                }
            }

            System.out.println(details.toString(4));
            return 0;

        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", "Extraction failed");
            error.put("message", e.getMessage());
            if (e.getCause() != null) {
                error.put("cause", e.getCause().toString());
            }
            System.err.println(error.toString(4));
            return 1;
        }
    }
}