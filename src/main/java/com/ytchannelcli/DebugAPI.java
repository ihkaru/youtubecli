package com.ytchannelcli;

import java.lang.reflect.Method;
import java.util.List;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

import com.ytchannelcli.App;

public class DebugAPI {

    public static void main(String[] args) {
        try {
            NewPipe.init(new App.SimpleDownloader());

            if (args.length > 0) {
                System.out.println("=== Testing with actual channel ===");
                ChannelExtractor extractor = NewPipe.getService(0)
                        .getChannelExtractor(args[0]);
                extractor.fetchPage();

                System.out.println("Channel name: " + extractor.getName());
                System.out.println("Channel ID: " + extractor.getId());

                System.out.println("\n=== Exploring getTabs() ===");
                List<ListLinkHandler> tabs = extractor.getTabs();
                System.out.println("Number of tabs: " + tabs.size());

                for (int i = 0; i < tabs.size(); i++) {
                    ListLinkHandler tab = tabs.get(i);
                    System.out.println("\nTab " + i + ":");
                    System.out.println("  URL: " + tab.getUrl());
                    System.out.println("  ID: " + tab.getId());
                    System.out.println("  Content filters: " + tab.getContentFilters());

                    // Try to get tab extractor
                    try {
                        ChannelTabExtractor tabExtractor = NewPipe.getService(0)
                                .getChannelTabExtractor(tab);
                        tabExtractor.fetchPage();

                        System.out.println("  Tab name: " + tabExtractor.getName());

                        // Look for methods that might give us videos
                        System.out.println("  Available methods:");
                        for (Method method : tabExtractor.getClass().getMethods()) {
                            String name = method.getName();
                            if (method.getDeclaringClass() == Object.class)
                                continue;
                            if (name.toLowerCase().contains("item") ||
                                    name.toLowerCase().contains("page") ||
                                    name.toLowerCase().contains("stream") ||
                                    name.toLowerCase().contains("video")) {
                                System.out.println(
                                        "    " + method.getName() + "() -> " + method.getReturnType().getSimpleName());
                            }
                        }

                        // Try to get initial page
                        try {
                            Method m = tabExtractor.getClass().getMethod("getInitialPage");
                            Object result = m.invoke(tabExtractor);
                            System.out.println("  ✓ getInitialPage() works!");
                            System.out.println("    Returns: " + result.getClass().getName());

                            // Try to get items from the page
                            Method getItems = result.getClass().getMethod("getItems");
                            Object items = getItems.invoke(result);
                            System.out.println("    Items type: " + items.getClass().getName());

                            if (items instanceof List) {
                                List<?> itemList = (List<?>) items;
                                System.out.println("    Number of items: " + itemList.size());
                                if (!itemList.isEmpty()) {
                                    System.out.println("    First item type: " + itemList.get(0).getClass().getName());
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("  ✗ Error with getInitialPage(): " + e.getMessage());
                        }

                    } catch (Exception e) {
                        System.out.println("  Error getting tab extractor: " + e.getMessage());
                    }
                }

            } else {
                System.out.println("Usage: provide a YouTube channel URL as argument");
                System.out.println("Example: https://www.youtube.com/channel/UCBR8-60-B28hp2BmDPdntcQ");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}