package com.ytchannelcli.commands;

import java.util.concurrent.Callable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-videos", mixinStandardHelpOptions = true, description = "Fetches the latest videos from a YouTube channel.")
public class GetVideos implements Callable<Integer> {

    @Option(names = { "--url" }, required = true, description = "The URL of the YouTube channel.")
    private String channelUrl;

    @Option(names = { "--limit" }, description = "The maximum number of videos to fetch.", defaultValue = "5")
    private int limit;

    @Option(names = {
            "--tab" }, description = "Tab to fetch from: videos, shorts, livestreams (default: videos)", defaultValue = "videos")
    private String tabFilter;

    @Override
    public Integer call() {
        try {
            ChannelExtractor extractor = NewPipe.getService(0)
                    .getChannelExtractor(channelUrl);
            extractor.fetchPage();

            // Find the tab we want
            ListLinkHandler targetTab = null;
            for (ListLinkHandler tab : extractor.getTabs()) {
                if (tab.getContentFilters().contains(tabFilter)) {
                    targetTab = tab;
                    break;
                }
            }

            if (targetTab == null) {
                JSONObject error = new JSONObject();
                error.put("error", "Tab not found");
                error.put("message", "Tab '" + tabFilter + "' not found in channel");
                error.put("available_tabs", extractor.getTabs().stream()
                        .flatMap(t -> t.getContentFilters().stream())
                        .toArray());
                System.err.println(error.toString(4));
                return 1;
            }

            // Get the tab extractor
            ChannelTabExtractor tabExtractor = NewPipe.getService(0)
                    .getChannelTabExtractor(targetTab);
            tabExtractor.fetchPage();

            // Get items from the tab (returns InfoItem, not StreamInfoItem)
            InfoItemsPage<InfoItem> page = tabExtractor.getInitialPage();
            JSONArray videos = new JSONArray();

            for (InfoItem item : page.getItems()) {
                if (videos.length() >= limit) {
                    break;
                }

                // Only process StreamInfoItem (skip playlists, channels, etc)
                if (!(item instanceof StreamInfoItem)) {
                    continue;
                }

                StreamInfoItem streamItem = (StreamInfoItem) item;

                JSONObject video = new JSONObject();
                video.put("title", streamItem.getName());
                video.put("url", streamItem.getUrl());
                video.put("is_short", streamItem.getUrl().contains("/shorts/"));

                try {
                    if (!streamItem.getThumbnails().isEmpty()) {
                        video.put("thumbnail_url", streamItem.getThumbnails().get(0).getUrl());
                    }
                } catch (Exception e) {
                    /* ignore */
                }

                try {
                    long duration = streamItem.getDuration();
                    if (duration > 0) {
                        video.put("duration_seconds", duration);
                    }
                } catch (Exception e) {
                    /* ignore */
                }

                try {
                    long viewCount = streamItem.getViewCount();
                    if (viewCount >= 0) {
                        video.put("view_count", viewCount);
                    }
                } catch (Exception e) {
                    /* ignore */
                }

                try {
                    if (streamItem.getUploadDate() != null) {
                        video.put("upload_date", streamItem.getUploadDate().offsetDateTime().toString());
                    }
                } catch (Exception e) {
                    /* ignore */
                }

                videos.put(video);
            }

            System.out.println(videos.toString(4));
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