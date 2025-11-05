# Project Status: YTChannelCLI

This document summarizes the current status of the `yt-cli` project as of October 9, 2025.

## Summary

The project goal was to create a command-line tool to extract data from YouTube. A functional JAR file (`yt-cli-1.0.0.jar`) has been successfully built and is located in the `build/libs/` directory.

The core functionality for fetching channel videos is **fully working**, including support for different content types (regular videos, shorts, and livestreams).

## Command Status

### Working Features

- `get-videos`: **✅ WORKING**

  - **Description**: Fetches a list of the latest videos from a YouTube channel with support for different content tabs.
  - **Features**:
    - Fetches regular videos, shorts, or livestreams.
    - Returns comprehensive metadata including title, URL, thumbnail, duration, view count, and upload date.
    - Configurable limit for the number of items to fetch.
    - Works with classic channel URL format (`https://www.youtube.com/channel/CHANNEL_ID`).
  - **Example Usage**:

    ```bash
    # Get regular videos (default tab)
    java -jar build/libs/yt-cli-1.0.0.jar get-videos --url "https://www.youtube.com/channel/UCBR8-60-B28hp2BmDPdntcQ" --limit 5

    # Get shorts
    java -jar build/libs/yt-cli-1.0.0.jar get-videos --url "https://www.youtube.com/channel/UCBR8-60-B28hp2BmDPdntcQ" --limit 5 --tab shorts

    # Get livestreams
    java -jar build/libs/yt-cli-1.0.0.jar get-videos --url "https://www.youtube.com/channel/UCBR8-60-B28hp2BmDPdntcQ" --limit 5 --tab livestreams
    ```

  - **Output Format**: JSON array with video objects containing:
    - `title`: Video title
    - `url`: Video URL
    - `is_short`: Boolean indicating if it's a YouTube Short
    - `thumbnail_url`: Thumbnail image URL
    - `duration_seconds`: Video duration (when available)
    - `view_count`: Number of views (when available)
    - `upload_date`: Upload date in ISO 8601 format (when available)

### Non-Working Features

- `search`: **❌ NOT WORKING**

  - **Description**: Was intended to search for videos on YouTube.
  - **Status**: The YouTube search API has changed significantly since NewPipeExtractor v0.24.8 was released. This feature requires a newer version of the library.
  - **Error**: Fails at runtime with a "Got HTML document, expected JSON response" error.

- `get-details`: **❌ NOT WORKING**
  - **Description**: Was intended to fetch detailed information and transcripts for a single video.
  - **Status**: Similar to `search`, the video details API has changed and requires a newer version of NewPipeExtractor.
  - **Error**: Fails at runtime with the same "Got HTML document, expected JSON response" error.

### Unimplemented Features

- `get-playlist`: **⏸️ NOT IMPLEMENTED**
  - **Description**: This command was planned to fetch videos from a YouTube playlist.
  - **Status**: Not implemented yet, but could be implemented using the same pattern as `get-videos` since NewPipeExtractor v0.24.8 supports playlist extraction through channel tabs.

## Technical Details

### Implementation Approach

The project successfully implements a custom `Downloader` class that bridges NewPipeExtractor with OkHttp for HTTP requests. This implementation supports:

- GET and POST requests
- Custom headers
- Request body handling for POST requests
- Proper response parsing

### API Discovery Process

During development, we used Java reflection to discover available methods in the NewPipeExtractor API:

- Created a `DebugAPI.java` utility to introspect class methods at runtime.
- Discovered that channels use a tab-based structure (`getTabs()`).
- Each tab provides access to different content types (videos, shorts, livestreams, playlists).
- Tab extractors return `InfoItemsPage<InfoItem>` which can contain various item types.

This debugging approach proved essential for understanding the actual API structure versus documentation.

### Dependencies

- `NewPipeExtractor v0.24.8`: Core YouTube extraction library (via JitPack)
- `Picocli 4.7.5`: Command-line interface framework
- `OkHttp 4.12.0`: HTTP client for network requests
- `org.json 20231013`: JSON parsing and generation

### Known Limitations

- **Channel URL Format**: Only works with classic channel URLs (`https://www.youtube.com/channel/CHANNEL_ID`). The newer `@handle` format is not supported in this version.
- **Library Version**: Locked to NewPipeExtractor v0.24.8 due to JitPack availability issues with newer versions (v0.26.x).
- **YouTube API Changes**: `search` and `get-details` features are broken due to YouTube API changes that occurred after v0.24.8 was released.

## Building the Project

```bash
# Build JAR file
.\gradlew.bat shadowJar

# Output location
build/libs/yt-cli-1.0.0.jar
```

## Future Improvements

If newer versions of NewPipeExtractor become available:

- Update dependency to v0.26.x or later.
- Re-enable `search` and `get-details` commands.
- Add support for `@handle` channel format.
- Implement `get-playlist` command.
- Add pagination support for fetching more than the initial page of results.

## Conclusion

The project successfully achieves its primary goal of fetching video lists from YouTube channels. The working `get-videos` command provides comprehensive metadata and supports multiple content types (videos, shorts, livestreams), making it a useful tool for YouTube data extraction despite the limitations with other planned features.
