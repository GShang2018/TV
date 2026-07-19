package com.fongmi.btengine;

import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Open-source BT engine extractor using libtorrent4j.
 * Implements Source.Extractor to handle magnet links with full DHT/PEX support.
 * <p>
 * This extractor replaces the old aria2-based implementation.
 * libtorrent4j runs in-process (no external process needed) and supports
 * all Android architectures including x86.
 */
public class BtExtractor {

    private static final String TAG = BtExtractor.class.getSimpleName();

    private final LibtorrentSession rpc;
    private String currentGid;

    public BtExtractor() {
        this.rpc = new LibtorrentSession();
    }

    /**
     * Check if this extractor can handle the given URL scheme.
     */
    public boolean match(String scheme, String host) {
        return "magnet".equals(scheme);
    }

    /**
     * Fetch a playable URL from a magnet link using libtorrent4j.
     * This blocks until the magnet metadata is resolved and a file is available.
     */
    public String fetch(String url) throws Exception {
        // Ensure libtorrent4j session is running
        if (!LibtorrentEngine.get().isRunning()) {
            throw new IOException("libtorrent4j session is not running");
        }

        Log.e(TAG, "Fetching magnet: " + url);

        // Add the magnet link and wait for metadata
        String infoHash = rpc.addMagnet(url);
        if (infoHash == null) {
            throw new IOException("Failed to resolve magnet metadata: " + url);
        }
        currentGid = infoHash;

        // Wait for a file to be available
        String filePath = rpc.waitForFile(infoHash);
        if (filePath == null) {
            Log.e(TAG, "waitForFile returned null for: " + infoHash);
            throw new IOException("Failed to download file from magnet: " + url);
        }

        Log.e(TAG, "Resolved file: " + filePath);
        
        // Check if the file actually exists and has data
        File resolvedFile = new File(filePath);
        Log.e(TAG, "File exists: " + resolvedFile.exists() + ", length: " + resolvedFile.length()
            + ", canRead: " + resolvedFile.canRead());
        
        // If file doesn't exist or is empty, log warning but still return the path
        // The download might still be in progress
        if (!resolvedFile.exists()) {
            Log.e(TAG, "WARNING: Resolved file does not exist yet!");
        } else if (resolvedFile.length() == 0) {
            Log.e(TAG, "WARNING: Resolved file is empty (0 bytes)!");
        } else {
            // Read the first 16 bytes to determine the actual file format
            // This helps diagnose container parsing issues in ExoPlayer
            try {
                java.io.RandomAccessFile raf = new java.io.RandomAccessFile(resolvedFile, "r");
                byte[] header = new byte[16];
                int bytesRead = raf.read(header);
                raf.close();
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < bytesRead; i++) {
                    hex.append(String.format("%02X ", header[i]));
                }
                Log.e(TAG, "File header (first " + bytesRead + " bytes): " + hex.toString().trim());
                // Check for common container format signatures
                if (bytesRead >= 4) {
                    if (header[0] == 0x1A && header[1] == 0x45 && header[2] == 0xDF && header[3] == 0xA3) {
                        Log.e(TAG, "Container format: Matroska/WebM (EBML header)");
                    } else if (header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
                        Log.e(TAG, "Container format: MP4 (ftyp box at offset 4)");
                    } else if (header[0] == 0x00 && header[1] == 0x00 && header[2] == 0x00 && header[3] == 0x1C) {
                        Log.e(TAG, "Container format: MP4 (ftyp box at offset 0, size=0x1C)");
                    } else if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46) {
                        Log.e(TAG, "Container format: AVI (RIFF header)");
                    } else if (header[0] == 0x47 && header[1] == 0x00 && header[2] == 0x00 && header[3] == 0x00) {
                        Log.e(TAG, "Container format: MPEG-TS");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading file header", e);
            }
        }
        
        // Return the raw file path (not URI-encoded) so that:
        // 1. Source.fetch() can detect resolution succeeded (path != magnet://) -> parse=0
        // 2. Players.isIllegal() can check file existence via Path.exists() which uses
        //    new File(path) - this does NOT decode URI encoding like %E9%80%86
        // 3. Players.setMediaSource() can identify it as a local file (starts with /)
        //    and use file:// URI for direct FileDataSource access.
        // IMPORTANT: Do not use Uri.fromFile().toString() here because it URI-encodes
        // Chinese characters (e.g. 逆行 -> %E9%80%86%E8%A1%8C), which causes
        // Path.exists() -> new File(path) to fail since File doesn't decode URI encoding.
        Log.e(TAG, "Returning raw file path: " + resolvedFile.getAbsolutePath());
        return resolvedFile.getAbsolutePath();
    }

    /**
     * Stop the current download task.
     */
    public void stop() {
        if (currentGid != null) {
            try {
                rpc.forceRemove(currentGid);
            } catch (Exception e) {
                Log.w(TAG, "Error stopping download", e);
            }
            currentGid = null;
        }
    }

    /**
     * Clean up resources.
     */
    public void exit() {
        stop();
        try {
            rpc.purgeDownloadResult();
        } catch (Exception e) {
            Log.w(TAG, "Error purging download results", e);
        }
    }

    /**
     * Get the list of resolved episodes from a magnet link.
     * Used for batch parsing in Source.parse().
     */
    public static class Parser {

        /**
         * Check if the URL can be handled by this parser.
         */
        public static boolean match(String url) {
            return url != null && url.startsWith("magnet:");
        }

        /**
         * Resolve a magnet link and return episode info.
         * This is a blocking call that waits for libtorrent4j to resolve metadata.
         */
        public static List<Episode> parse(String url) {
            List<Episode> episodes = new ArrayList<>();
            try {
                // Ensure libtorrent4j is running
                if (!LibtorrentEngine.get().isRunning()) {
                    Log.w(TAG, "libtorrent4j not running, cannot parse magnet");
                    return episodes;
                }

                BtExtractor extractor = new BtExtractor();
                String fileUrl = extractor.fetch(url);
                if (fileUrl != null) {
                    File file = new File(Uri.parse(fileUrl).getPath());
                    String name = file.getName();
                    long size = file.length();
                    episodes.add(Episode.create(name, size, fileUrl));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing magnet: " + url, e);
            }
            return episodes;
        }
    }

    /**
     * Simple Episode data class matching the app's Episode bean structure.
     */
    public static class Episode {
        public String name;
        public long size;
        public String url;

        public static Episode create(String name, long size, String url) {
            Episode ep = new Episode();
            ep.name = name;
            ep.size = size;
            ep.url = url;
            return ep;
        }

        public static Episode create(String name, String url) {
            return create(name, 0, url);
        }
    }
}
