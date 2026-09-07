package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.web.WebMpd;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * 提供 WebHome 多集推送 data-URI MPD 的 http 访问端点。
 */
public class WebMpdProcess implements Process {

    @Override
    public boolean isRequest(IHTTPSession session, String path) {
        return path.startsWith("/webMpd");
    }

    @Override
    public Response doResponse(IHTTPSession session, String path, Map<String, String> files) {
        String id = session.getParms().get("m");
        if (id == null) {
            int start = path.indexOf("/webMpd/");
            int end = path.endsWith(".mpd") ? path.length() - ".mpd".length() : path.length();
            id = start >= 0 && end > start ? path.substring(start + "/webMpd/".length(), end) : "";
        }
        String mpd = WebMpd.get(id);
        if (mpd == null) return Nano.error(Response.Status.NOT_FOUND, "mpd not found");
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/dash+xml; charset=utf-8", mpd);
    }
}
