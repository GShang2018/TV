package com.fongmi.btengine;

import android.util.Log;

import com.github.catvod.utils.Prefers;

import org.libtorrent4j.AlertListener;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.SessionParams;
import org.libtorrent4j.SettingsPack;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.AlertType;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.MetadataReceivedAlert;
import org.libtorrent4j.alerts.TorrentFinishedAlert;
import org.libtorrent4j.alerts.TorrentRemovedAlert;
import org.libtorrent4j.alerts.TorrentErrorAlert;
import org.libtorrent4j.alerts.DhtBootstrapAlert;
import org.libtorrent4j.alerts.ListenFailedAlert;
import org.libtorrent4j.alerts.ListenSucceededAlert;
import org.libtorrent4j.alerts.ExternalIpAlert;
import org.libtorrent4j.alerts.PortmapAlert;
import org.libtorrent4j.alerts.PortmapErrorAlert;
import org.libtorrent4j.alerts.DhtGetPeersAlert;
import org.libtorrent4j.alerts.DhtReplyAlert;
import org.libtorrent4j.alerts.PeerConnectAlert;
import org.libtorrent4j.alerts.TrackerAnnounceAlert;
import org.libtorrent4j.alerts.TrackerReplyAlert;
import org.libtorrent4j.alerts.TrackerErrorAlert;
import org.libtorrent4j.alerts.TrackerWarningAlert;
import org.libtorrent4j.swig.settings_pack;

import java.io.File;

/**
 * libtorrent4j session manager.
 * Replaces the old aria2 process-based engine with a native in-process BT engine.
 * Supports all Android architectures including x86.
 */
public class LibtorrentEngine {

    private static final String TAG = LibtorrentEngine.class.getSimpleName();

    private SessionManager session;
    private boolean running;

    private static class Loader {
        static volatile LibtorrentEngine INSTANCE = new LibtorrentEngine();
    }

    public static LibtorrentEngine get() {
        return Loader.INSTANCE;
    }

    /**
     * Default trackers for finding peers when DHT is not enough.
     */
    private static final String[] DEFAULT_TRACKERS = {
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://tracker.moeking.me:6969/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://open.demonii.com:1337/announce",
        "udp://tracker.cyberia.is:6969/announce",
        "udp://tracker.dler.org:6969/announce",
        "https://tracker.nanoha.org:443/announce",
        "https://tracker.lilithraws.org:443/announce",
        "http://tracker.bt4g.com:2095/announce",
        "http://tracker.files.fm:6969/announce",
        "udp://tracker.leechers-paradise.org:6969/announce",
        "udp://tracker.coppersurfer.tk:6969/announce",
        "udp://tracker.pirateparty.gr:6969/announce",
        "udp://tracker.zer0day.to:1337/announce"
    };

    /**
     * Start the libtorrent4j session.
     */
    public synchronized void start(String trackers) {
        try {
            if (running) {
                stop();
            }

            session = new SessionManager();

            // Configure session parameters
            SessionParams params = new SessionParams();
            SettingsPack settings = params.getSettings();

            // ========== Rate limits (0 = unlimited) ==========
            settings.setInteger(settings_pack.int_types.download_rate_limit.swigValue(), 0);
            settings.setInteger(settings_pack.int_types.upload_rate_limit.swigValue(), 0);

            // ========== Connection limits ==========
            settings.setInteger(settings_pack.int_types.active_downloads.swigValue(), 5);
            settings.setInteger(settings_pack.int_types.active_seeds.swigValue(), 0);
            settings.setInteger(settings_pack.int_types.active_limit.swigValue(), 10);
            settings.setInteger(settings_pack.int_types.connections_limit.swigValue(), 200);
            settings.setInteger(settings_pack.int_types.max_peerlist_size.swigValue(), 1000);

            // ========== Listen interfaces for incoming connections ==========
            // libtorrent needs to listen on a port to receive incoming peer connections.
            // Without this, it can only make outgoing connections.
            // Format: "ip:port" or "0.0.0.0:6881" for all interfaces
            settings.setString(settings_pack.string_types.listen_interfaces.swigValue(), "0.0.0.0:6881");
            // Set outgoing port (0 = random)
            settings.setInteger(settings_pack.int_types.outgoing_port.swigValue(), 0);

            // ========== Enable TCP and uTP for both incoming and outgoing ==========
            settings.setBoolean(settings_pack.bool_types.enable_incoming_tcp.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.enable_outgoing_tcp.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.enable_incoming_utp.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.enable_outgoing_utp.swigValue(), true);

            // ========== Enable DHT, LSD, PEX, NAT-PMP, UPnP ==========
            settings.setBoolean(settings_pack.bool_types.enable_dht.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.enable_lsd.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), true);

            // ========== DHT settings ==========
            settings.setString(settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                    "router.bittorrent.com:6881,dht.transmissionbt.com:6881,router.utorrent.com:6881,dht.aelitis.com:6881");
            settings.setInteger(settings_pack.int_types.dht_max_peers.swigValue(), 200);

            // ========== Encryption settings ==========
            // Allow both encrypted and plaintext connections for maximum compatibility
            // allowed_enc_level: 0=plaintext, 1=rc4, 2=both
            settings.setInteger(settings_pack.int_types.allowed_enc_level.swigValue(), 2);
            // in_enc_policy / out_enc_policy: 0=disabled, 1=enabled, 2=forced
            settings.setInteger(settings_pack.int_types.in_enc_policy.swigValue(), 1);
            settings.setInteger(settings_pack.int_types.out_enc_policy.swigValue(), 1);
            settings.setBoolean(settings_pack.bool_types.prefer_rc4.swigValue(), true);

            // ========== Peer settings ==========
            settings.setInteger(settings_pack.int_types.max_failcount.swigValue(), 3);
            settings.setInteger(settings_pack.int_types.peer_timeout.swigValue(), 30);
            settings.setInteger(settings_pack.int_types.tick_interval.swigValue(), 1000);
            settings.setInteger(settings_pack.int_types.inactivity_timeout.swigValue(), 60);

            // ========== Tracker announce optimization (reference: libretorrent) ==========
            // Announce to ALL trackers in ALL tiers simultaneously.
            // Without this, libtorrent only announces to the first tracker in the first tier,
            // wasting all additional trackers. This is THE key setting for tracker acceleration.
            settings.setBoolean(settings_pack.bool_types.announce_to_all_trackers.swigValue(), true);
            settings.setBoolean(settings_pack.bool_types.announce_to_all_tiers.swigValue(), true);
            // Don't validate HTTPS tracker certificates (connect to more trackers)
            settings.setBoolean(settings_pack.bool_types.validate_https_trackers.swigValue(), false);

            // ========== Session optimization ==========
            // Alert queue size (default is too small, may lose alerts)
            settings.setInteger(settings_pack.int_types.alert_queue_size.swigValue(), 5000);
            // Disable IP notifier (not needed on Android)
            settings.setBoolean(settings_pack.bool_types.enable_ip_notifier.swigValue(), false);

            // ========== User agent ==========
            settings.setString(settings_pack.string_types.user_agent.swigValue(), "libtorrent4j/2.1.0");

            // ========== Apply settings ==========
            params.setSettings(settings);

            // Add alert listener for comprehensive logging
            session.addListener(new AlertListener() {
                @Override
                public int[] types() {
                    return new int[]{
                            AlertType.ADD_TORRENT.swig(),
                            AlertType.METADATA_RECEIVED.swig(),
                            AlertType.TORRENT_FINISHED.swig(),
                            AlertType.TORRENT_REMOVED.swig(),
                            AlertType.TORRENT_ERROR.swig(),
                            AlertType.DHT_BOOTSTRAP.swig(),
                            AlertType.LISTEN_FAILED.swig(),
                            AlertType.LISTEN_SUCCEEDED.swig(),
                            AlertType.EXTERNAL_IP.swig(),
                            AlertType.PORTMAP.swig(),
                            AlertType.PORTMAP_ERROR.swig(),
                            AlertType.DHT_GET_PEERS.swig(),
                            AlertType.DHT_REPLY.swig(),
                            AlertType.PEER_CONNECT.swig(),
                            AlertType.TRACKER_ANNOUNCE.swig(),
                            AlertType.TRACKER_REPLY.swig(),
                            AlertType.TRACKER_ERROR.swig(),
                            AlertType.TRACKER_WARNING.swig()
                    };
                }

                @Override
                public void alert(Alert<?> alert) {
                    switch (alert.type()) {
                        case ADD_TORRENT: {
                            AddTorrentAlert a = (AddTorrentAlert) alert;
                            String name = a.handle() != null ? a.handle().status().name() : "unknown";
                            Log.e(TAG, "Torrent added: " + name);
                            break;
                        }
                        case METADATA_RECEIVED: {
                            MetadataReceivedAlert a = (MetadataReceivedAlert) alert;
                            String name = a.handle() != null ? a.handle().status().name() : "unknown";
                            Log.e(TAG, "Metadata received for: " + name);
                            break;
                        }
                        case TORRENT_FINISHED: {
                            TorrentFinishedAlert a = (TorrentFinishedAlert) alert;
                            String name = a.handle() != null ? a.handle().status().name() : "unknown";
                            Log.e(TAG, "Torrent finished: " + name);
                            break;
                        }
                        case TORRENT_REMOVED:
                            Log.e(TAG, "Torrent removed");
                            break;
                        case TORRENT_ERROR: {
                            TorrentErrorAlert a = (TorrentErrorAlert) alert;
                            Log.e(TAG, "Torrent error: " + a.message() + ", error: " + a.error());
                            break;
                        }
                        case DHT_BOOTSTRAP: {
                            DhtBootstrapAlert a = (DhtBootstrapAlert) alert;
                            Log.e(TAG, "DHT bootstrap: " + a.message());
                            break;
                        }
                        case LISTEN_FAILED: {
                            ListenFailedAlert a = (ListenFailedAlert) alert;
                            Log.e(TAG, "Listen failed: " + a.message());
                            break;
                        }
                        case LISTEN_SUCCEEDED: {
                            ListenSucceededAlert a = (ListenSucceededAlert) alert;
                            Log.e(TAG, "Listen succeeded: " + a.message());
                            break;
                        }
                        case EXTERNAL_IP: {
                            ExternalIpAlert a = (ExternalIpAlert) alert;
                            Log.e(TAG, "External IP: " + a.externalAddress());
                            break;
                        }
                        case PORTMAP:
                            Log.e(TAG, "UPnP/NAT-PMP: " + alert.message());
                            break;
                        case PORTMAP_ERROR:
                            Log.e(TAG, "UPnP/NAT-PMP error: " + alert.message());
                            break;
                        case DHT_GET_PEERS:
                            Log.e(TAG, "DHT get peers: " + alert.message());
                            break;
                        case DHT_REPLY:
                            Log.e(TAG, "DHT reply: " + alert.message());
                            break;
                        case PEER_CONNECT:
                            Log.e(TAG, "Peer connect: " + alert.message());
                            break;
                        case TRACKER_ANNOUNCE: {
                            TrackerAnnounceAlert a = (TrackerAnnounceAlert) alert;
                            Log.e(TAG, "Tracker>> announce: " + a.trackerUrl());
                            break;
                        }
                        case TRACKER_REPLY: {
                            TrackerReplyAlert a = (TrackerReplyAlert) alert;
                            Log.e(TAG, "Tracker<< reply: " + a.trackerUrl() + " peers=" + a.numPeers());
                            break;
                        }
                        case TRACKER_ERROR: {
                            TrackerErrorAlert a = (TrackerErrorAlert) alert;
                            Log.e(TAG, "Tracker!! error: " + a.trackerUrl() + " msg=" + a.errorMessage());
                            break;
                        }
                        case TRACKER_WARNING: {
                            TrackerWarningAlert a = (TrackerWarningAlert) alert;
                            Log.e(TAG, "Tracker!! warning: " + a.trackerUrl() + " msg=" + a.message());
                            break;
                        }
                    }
                }
            });

            // Start the session
            session.start(params);
            running = true;

            Log.e(TAG, "libtorrent4j session started successfully");

            // Trackers are added per-torrent in LibtorrentSession
            // (session_handle does not have add_tracker in this version)

        } catch (Exception e) {
            Log.e(TAG, "Failed to start libtorrent4j session", e);
            running = false;
            if (session != null) {
                try { session.stop(); } catch (Exception ignored) {}
                session = null;
            }
        }
    }

    public synchronized void start() {
        start("");
    }

    /**
     * Get the default trackers array for use by LibtorrentSession.
     */
    public static String[] getDefaultTrackers() {
        return DEFAULT_TRACKERS;
    }

    /**
     * Whether announce_to_all_trackers is enabled (hardcoded true since this optimization).
     * Used for logging so users can verify tracker acceleration is active.
     */
    public static boolean announceToAllEnabled() {
        return true;
    }

    /**
     * Parse tracker text: one URL per line, lines starting with # are ignored.
     */
    public static String[] parseTrackers(String text) {
        if (text == null || text.trim().isEmpty()) return new String[0];
        String[] lines = text.split("\n");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) result.add(t);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Get trackers from user preferences (key="tracker_list"),
     * falling back to DEFAULT_TRACKERS when user hasn't configured any.
     * btengine reads the same SharedPreferences as Setting.getTrackerList() in app module.
     */
    public static String[] getTrackers() {
        String text = Prefers.getString("tracker_list", "");
        String[] parsed = parseTrackers(text);
        return parsed.length > 0 ? parsed : DEFAULT_TRACKERS;
    }

    /**
     * Get DEFAULT_TRACKERS as newline-separated text for UI fallback.
     */
    public static String getDefaultTrackersText() {
        return String.join("\n", DEFAULT_TRACKERS);
    }


    /**
     * Stop the libtorrent4j session.
     */
    public synchronized void stop() {
        if (session != null) {
            try {
                session.stop();
            } catch (Exception ignored) {
            }
            session = null;
        }
        running = false;
        Log.e(TAG, "libtorrent4j session stopped");
    }

    public boolean isRunning() {
        return running && session != null && session.isRunning();
    }

    public SessionManager getSession() {
        return session;
    }

    private File getDownloadDir() {
        File dir = new File(com.github.catvod.Init.context().getCacheDir(), "libtorrent/downloads");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // Keep these for backward compatibility with BtExtractor
    public static String getRpcUrl() {
        return "";
    }

    public static String getRpcSecret() {
        return "";
    }

    public static int getRpcPort() {
        return 0;
    }
}
