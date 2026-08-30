package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.github.catvod.crawler.DebugLogStore;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * 调试日志网页：终端式日志流。
 * 端点：
 *  - /debug/logs      页面
 *  - /debug/stream    轮询 JSON（version 不变返回 text:null）
 *  - /debug/logs.txt  下载完整日志（UTF-8 BOM）
 *  - /debug/clear     清空后跳回页面
 */
public class DebugLogs implements Process {

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/debug/logs") || url.startsWith("/debug/stream") || url.startsWith("/debug/clear");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        if (url.startsWith("/debug/clear")) {
            DebugLogStore.clear();
            return noCache(NanoHTTPD.newFixedLengthResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, ""), "/debug/logs");
        }
        if (url.startsWith("/debug/stream")) return stream(session);
        if (url.startsWith("/debug/logs.txt")) return download();
        return page();
    }

    private Response page() {
        return noCache(NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html()), null);
    }

    private Response download() {
        String text = DebugLogStore.text();
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, data, 0, bom.length);
        System.arraycopy(content, 0, data, bom.length, content.length);
        Response response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", new ByteArrayInputStream(data), data.length);
        response.addHeader("Content-Disposition", "attachment; filename=tv-debug-log.txt");
        response.addHeader("X-Content-Type-Options", "nosniff");
        return noCache(response, null);
    }

    private Response stream(IHTTPSession session) {
        long version = DebugLogStore.version();
        boolean unchanged = version == paramLong(session, "v", -1);
        String text = unchanged ? null : DebugLogStore.text();
        String json = "{\"size\":" + DebugLogStore.size() + ",\"bytes\":" + DebugLogStore.bytes() + ",\"version\":" + version + ",\"text\":" + (unchanged ? "null" : "\"" + json(text) + "\"") + "}";
        return noCache(NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json), null);
    }

    private long paramLong(IHTTPSession session, String key, long fallback) {
        try {
            return Long.parseLong(session.getParms().get(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    private Response noCache(Response response, String location) {
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.addHeader("Pragma", "no-cache");
        if (!TextUtils.isEmpty(location)) response.addHeader("Location", location);
        return response;
    }

    private String html() {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<title>调试日志</title><style>" + css() + "</style></head><body>"
                + "<header><div class=\"wrap\">"
                + "<div class=\"bar\"><h1>调试日志</h1><span id=\"meta\" class=\"meta\"></span></div>"
                + "<div class=\"row\"><div id=\"levels\" class=\"levels\"></div></div>"
                + "<div class=\"row\"><input id=\"q\" placeholder=\"检索关键词...\" autocomplete=\"off\" spellcheck=\"false\">"
                + "<button id=\"pause\">暂停</button><button id=\"copy\">复制筛选</button>"
                + "<a class=\"btn\" href=\"/debug/logs.txt\" download=\"tv-debug-log.txt\">导出</a>"
                + "<a class=\"btn danger\" href=\"/debug/clear\">清空</a></div>"
                + "</div></header>"
                + "<main id=\"out\" class=\"wrap\"></main>"
                + "<script>" + script() + "</script></body></html>";
    }

    private String css() {
        return "html,body{box-sizing:border-box;width:100%;max-width:100%;margin:0;background:#0d1117;color:#c9d1d9;font:14px/1.6 -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}*,*:before,*:after{box-sizing:inherit}html,body{height:100%;overflow:hidden}body{display:flex;flex-direction:column}"
                + ".wrap{box-sizing:border-box;width:100%;max-width:1280px;margin:0 auto}"
                + "header{box-sizing:border-box;flex:0 0 auto;width:100%;max-width:100%;background:#161b22;border-bottom:1px solid #30363d;padding:10px 14px}"
                + ".bar{display:flex;align-items:center;gap:8px}h1{margin:0 10px 0 0;font-size:16px;font-weight:650;white-space:nowrap}.meta{margin-left:auto;color:#8b949e;font-size:12px;white-space:nowrap}"
                + ".row{display:flex;flex-wrap:wrap;gap:6px;align-items:center;margin-top:8px}.row:first-child{margin-top:0}.levels{display:flex;flex-wrap:wrap;gap:6px}"
                + "button,.btn{appearance:none;border:1px solid #30363d;border-radius:6px;background:#21262d;color:#c9d1d9;padding:5px 10px;text-decoration:none;font:inherit;font-size:13px;cursor:pointer;white-space:nowrap;display:inline-flex;align-items:center;gap:6px}button.on{background:#1f6feb;border-color:#1f6feb;color:#fff}button i{font-style:normal;color:#8b949e;font-size:11px}button.on i{color:#cfe3ff}.btn.danger{color:#f85149}"
                + "input{flex:1 1 200px;min-width:120px;background:#0d1117;border:1px solid #30363d;border-radius:6px;color:#e6edf3;padding:6px 10px;font:inherit;font-size:13px;outline:none}input:focus{border-color:#1f6feb}"
                + "main{box-sizing:border-box;flex:1 1 auto;width:100%;max-width:100%;overflow-y:auto;overflow-x:hidden;padding:8px 14px 40px;-webkit-overflow-scrolling:touch}"
                + ".e{box-sizing:border-box;display:flex;gap:8px;align-items:baseline;width:100%;min-width:0;padding:1px 10px;border-left:3px solid #30363d;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12.5px;line-height:1.55;white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-all}.e:hover{background:#161b22}"
                + ".e .l{flex:0 0 auto;font-weight:700}.e .b{flex:1 1 auto;min-width:0}.e .t,.e .h{color:#6e7681}"
                + ".e.D .l{color:#8b949e}.e.D{border-color:#484f58}.e.I .l{color:#58a6ff}.e.I{border-color:#1f6feb}.e.W .l{color:#d29922}.e.W{border-color:#9e6a03;background:rgba(210,153,34,.06)}.e.E .l{color:#f85149}.e.E{border-color:#da3633;background:rgba(248,81,73,.08)}"
                + "mark{background:#d29922;color:#0d1117;padding:0 1px;border-radius:2px}"
                + ".empty{padding:30px;text-align:center;color:#6e7681}"
                + "@media(max-width:680px){header{padding:8px 10px}.e{padding:1px 6px;font-size:12px}.meta{display:none}}";
    }

    private String script() {
        return "const view=document.getElementById('out'),meta=document.getElementById('meta'),levels=document.getElementById('levels'),q=document.getElementById('q'),pause=document.getElementById('pause'),copy=document.getElementById('copy');"
                + "let ver=0,text='',lvl='',lastRaw='',stick=true,paused=false;"
                + "const esc=s=>String(s==null?'':s).replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[c]));"
                + "const escRe=s=>s.replace(/[.*+?^${}()|[\\]\\\\]/g,'\\\\$&');"
                + "view.addEventListener('scroll',()=>{stick=view.scrollTop+view.clientHeight>=view.scrollHeight-40},{passive:true});"
                + "function hl(s,k){const e=esc(s);if(!k)return e;return e.replace(new RegExp(escRe(k),'gi'),m=>'<mark>'+m+'</mark>')}"
                + "function parse(){const rows=[];let cur=null;for(const raw of text.split('\\n')){if(/^\\[[DIWE]\\] /.test(raw)){cur=[raw[1],raw];rows.push(cur)}else if(cur){cur[1]+='\\n'+raw}else if(raw){rows.push(['',raw])}}return rows}"
                + "function render(){"
                + "const key=q.value.trim().toLowerCase();const rows=parse();const cnt={'':rows.length,D:0,I:0,W:0,E:0};rows.forEach(r=>cnt[r[0]]++);"
                + "levels.innerHTML=['','D','I','W','E'].map(k=>'<button data-lvl=\"'+k+'\" class=\"'+(k===lvl?'on':'')+'\">'+(k?'['+k+']':'全部')+'<i>'+cnt[k]+'</i></button>').join('');"
                + "Array.from(levels.children).forEach(b=>b.onclick=()=>{lvl=b.dataset.lvl;render()});"
                + "const shown=rows.filter(r=>(!lvl||r[0]===lvl)&&(!key||r[1].toLowerCase().includes(key)));"
                + "lastRaw=shown.map(r=>r[1]).join('\\n');"
                + "view.innerHTML=shown.map(r=>{const m=/^\\[(.)\\] ([\\d\\-]+ [\\d:.]+) \\[([^\\]]*)\\] ([\\s\\S]*)$/.exec(r[1]);const lc=r[0]||'';if(m){return '<div class=\"e '+lc+'\"><span class=\"l '+lc+'\">['+lc+']</span><span class=\"b\">'+hl(m[2],key)+' <span class=\"h\">['+hl(m[3],key)+']</span> '+hl(m[4],key).replace(/\\n/g,'<br>')+'</span></div>'}return '<div class=\"e '+lc+'\">'+hl(r[1],key).replace(/\\n/g,'<br>')+'</div>'}).join('')||'<div class=\"empty\">没有匹配的日志</div>';"
                + "meta.textContent=rows.length+' 行 · '+(text.length/1024).toFixed(1)+' KB';"
                + "}"
                + "pause.onclick=()=>{paused=!paused;pause.textContent=paused?'继续':'暂停';pause.classList.toggle('on',paused)};"
                + "copy.onclick=()=>{const put=()=>{if(navigator.clipboard&&navigator.clipboard.writeText)return navigator.clipboard.writeText(lastRaw);return Promise.reject()};put().catch(()=>{const ta=document.createElement('textarea');ta.value=lastRaw;document.body.appendChild(ta);ta.select();document.execCommand('copy');ta.remove()})};"
                + "q.oninput=render;"
                + "async function poll(){if(!paused){try{const r=await fetch('/debug/stream?v='+ver+'&_='+Date.now(),{cache:'no-store'});const j=await r.json();if(j.version!==ver){ver=j.version||ver;text=j.text||'';render();if(stick)view.scrollTop=view.scrollHeight}}catch(e){}}setTimeout(poll,1500)}"
                + "render();poll();";
    }

    private String json(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}
