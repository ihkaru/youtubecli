package com.ytchannelcli.commands;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.search.SearchExtractor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "search", mixinStandardHelpOptions = true, description = "Searches for videos on YouTube.")
public class Search implements Callable<Integer> {

    @Option(names = { "--query" }, required = true, description = "The search query.")
    private String query;

    @Option(names = {
            "--filter" }, description = "The search filter (e.g., videos, playlists, channels).", defaultValue = "videos")
    private String filter;

    @Override
    public Integer call() {
        try {
            SearchExtractor extractor = NewPipe.getService(0) // 0 = YouTube
                    .getSearchExtractor(query, Collections.singletonList(filter), "");
            extractor.fetchPage();

            List<InfoItem> items = extractor.getInitialPage().getItems();
            JSONArray results = new JSONArray();

            for (InfoItem item : items) {
                JSONObject result = new JSONObject();
                result.put("title", item.getName());
                result.put("url", item.getUrl());
                results.put(result);
            }

            System.out.println(results.toString(4));
            return 0; // Success

        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", "Search failed");
            error.put("message", e.getMessage());
            if (e.getCause() != null) {
                error.put("cause", e.getCause().toString());
            }
            System.err.println(error.toString(4));
            return 1; // Failure
        }
    }
}