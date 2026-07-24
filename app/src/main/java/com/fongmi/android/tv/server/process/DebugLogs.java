package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.github.catvod.crawler.DebugLogStore;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

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
        Response response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html());
        return noCache(response, null);
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
        Response response = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json);
        return noCache(response, null);
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
        String logs = escape(DebugLogStore.text());
        String localUrl = "http://127.0.0.1:" + com.github.catvod.Proxy.getPort() + "/debug/logs";
        return "<!doctype html>"
                + "<html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<title>\u8c03\u8bd5\u65e5\u5fd7</title>"
                + "<style>" + css() + "</style></head><body>"
                + "<div class=\"header\"><section class=\"topbar\"><h1>\u8c03\u8bd5\u65e5\u5fd7</h1><a href=\"/debug/logs\">\u5237\u65b0</a><a id=\"download\" href=\"/debug/logs.txt\" download=\"tv-debug-log.txt\">\u4e0b\u8f7d</a><a href=\"/debug/clear\">\u6e05\u7a7a</a><span id=\"meta\" class=\"meta\">" + DebugLogStore.size() + " \u884c \u00b7 " + DebugLogStore.bytes() / 1024 + " KB</span></section>"
                + "<section class=\"tools\"><div class=\"chips\"><button class=\"chip on\" data-mode=\"all\">\u5168\u90e8</button><button class=\"chip\" data-mode=\"error\">\u9519\u8bef</button><button class=\"chip\" data-mode=\"quickjs\">QuickJS</button><button class=\"chip\" data-mode=\"py_spider\">Python</button><button class=\"chip\" data-mode=\"okhttp\">\u7f51\u7edc</button><button class=\"chip\" data-mode=\"server\">\u670d\u52a1</button><button class=\"chip\" data-mode=\"SpiderDebug\">\u722c\u866b</button><button class=\"chip\" data-mode=\"execute_error\">\u5f02\u5e38</button></div>"
                + "<div class=\"search\"><input id=\"filter\" placeholder=\"\u8fc7\u6ee4\u5173\u952e\u8bcd...\"><button id=\"pause\">\u6682\u505c</button></div><div id=\"summary\" class=\"summary\"></div></section></div>"
                + "<main><div id=\"logs\" class=\"logs\"></div><pre id=\"raw\" class=\"fallback\">" + logs + "</pre></main>"
                + "<script>" + script() + "</script>"
                + "</body></html>";
    }

    private String css() {
        return "html,body{box-sizing:border-box;width:100%;max-width:100%;margin:0;background:#f4f6f8;color:#1f2328;font:14px/1.5 -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;}*,*:before,*:after{box-sizing:inherit;min-width:0}html,body{height:100%;overflow:hidden}body{display:flex;flex-direction:column}"
                + ".topbar{box-sizing:border-box;display:flex;flex-wrap:wrap;gap:8px;align-items:center;width:100%;max-width:100%;overflow:hidden;margin:0;padding:8px 8px 0;background:#f4f6f8}"
                + "h1{margin:0 10px 0 0;font-size:17px;font-weight:650;white-space:nowrap}.meta{margin-left:auto;color:#656d76;font-size:12px;white-space:nowrap}"
                + "a,button{appearance:none;border:1px solid #d0d7de;border-radius:7px;background:#fff;color:#24292f;padding:6px 9px;text-decoration:none;font:inherit;cursor:pointer;white-space:nowrap}button.on,.chip.on{background:#0969da;border-color:#0969da;color:#fff}a:active,button:active{background:#eaeef2}"
                + ".header{box-sizing:border-box;flex:0 0 auto;width:100%;max-width:1280px;margin:0 auto;padding:0 8px;z-index:10}.header .topbar{border-radius:8px 8px 0 0;border:1px solid #d8dee4;border-bottom:none;box-shadow:0 2px 10px rgba(31,35,40,.04)}"
                + ".tools{box-sizing:border-box;width:100%;max-width:100%;overflow:hidden;margin:0;padding:8px;background:#fff;border:1px solid #d8dee4;border-top:none;border-radius:0 0 8px 8px;box-shadow:0 2px 10px rgba(31,35,40,.04)}.chips{display:flex;flex-wrap:wrap;gap:6px;overflow:hidden;padding-bottom:2px}.chip{flex:0 0 auto}.search{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:6px;align-items:center;margin-top:6px}input{box-sizing:border-box;width:100%;min-width:0;border:1px solid #d0d7de;border-radius:7px;padding:7px 9px;font:inherit}.summary{margin-top:6px;color:#57606a;font-size:12px;white-space:normal;overflow-wrap:anywhere;word-break:break-all}"
                + "main{box-sizing:border-box;flex:1 1 auto;width:100%;max-width:1280px;margin:0 auto;padding:8px;overflow-y:auto;overflow-x:hidden;-webkit-overflow-scrolling:touch}"
                + ".logs{box-sizing:border-box;display:grid;gap:8px;width:100%;max-width:100%;min-width:0}.fallback{box-sizing:border-box;max-width:100%;margin:0;background:#fff;border:1px solid #d8dee4;border-radius:8px;padding:10px;color:#57606a;font:12px/1.5 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-all}.entry{box-sizing:border-box;width:100%;max-width:100%;min-width:0;overflow:hidden;background:#fff;border:1px solid #d8dee4;border-radius:8px;padding:9px 10px}.entry.ok{border-left:4px solid #1a7f37}.entry.warn{border-left:4px solid #bf8700}.entry.err{border-left:4px solid #cf222e}.entry.raw{border-left:4px solid #8c959f}.top{display:flex;gap:8px;align-items:center;max-width:100%;min-width:0;overflow:hidden}.badge{flex:0 0 auto;border-radius:999px;padding:2px 7px;background:#eaeef2;color:#57606a;font-size:12px}.entry.ok .badge{background:#dafbe1;color:#116329}.entry.err .badge{background:#ffebe9;color:#cf222e}.title{min-width:0;font-weight:650;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.time{margin-left:auto;color:#8c959f;font-size:12px;white-space:nowrap}.detail{box-sizing:border-box;max-width:100%;min-width:0;overflow:hidden;margin-top:5px;color:#57606a;white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-all}.rawline{display:block;box-sizing:border-box;width:100%;max-width:100%;min-width:0;overflow:hidden;margin-top:6px;padding-top:6px;border-top:1px dashed #d8dee4;color:#6e7781;font:12px/1.45 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-all}"
                + "@media(max-width:680px){.topbar{padding:7px 8px 0}h1{font-size:16px}.meta{flex-basis:100%;margin-left:0}.search{grid-template-columns:minmax(0,1fr) auto}.title{white-space:normal}.time{display:none}.entry{padding:8px}.detail{font-size:13px}}";
    }

    private String script() {
        return "const rawEl=document.getElementById('raw'),logs=document.getElementById('logs'),meta=document.getElementById('meta'),summary=document.getElementById('summary'),filter=document.getElementById('filter'),pause=document.getElementById('pause'),download=document.getElementById('download');"
                + "let raw=rawEl.textContent,mode='all',paused=false,stick=true,lastVersion=0;"
                + "addEventListener('scroll',()=>{stick=(innerHeight+scrollY)>=(document.body.scrollHeight-80)},{passive:true});"
                + "document.querySelectorAll('.chip').forEach(b=>b.onclick=()=>{document.querySelectorAll('.chip').forEach(x=>x.classList.remove('on'));b.classList.add('on');mode=b.dataset.mode;render()});"
                + "filter.oninput=render;"
                + "pause.onclick=()=>{paused=!paused;pause.textContent=paused?'\u7ee7\u7eed':'\u6682\u505c';pause.classList.toggle('on',paused)};"
                + "download.onclick=()=>{paused=true;pause.textContent='\u7ee7\u7eed';pause.classList.add('on')};"
                + "function esc(s){return String(s||'').replace(/[&<>\"']/g,c=>({'&':'\u0026amp;','<':'\u0026lt;','>':'\u0026gt;','\"':'\u0026quot;',\"'\":'\u0026#39;'}[c]))}"
                + "function parse(line){const a=line.indexOf(' ['),b=line.indexOf('] ',a+2),c=line.indexOf(': ',b+2);return{line,time:a>0?line.slice(0,a):'',thread:a>0&&b>0?line.slice(a+2,b):'',tag:b>0&&c>0?line.slice(b+2,c):'',msg:c>0?line.slice(c+2):line}}"
                + "function explain(r){const low=(r.tag+': '+r.msg).toLowerCase();let e={kind:'raw',state:'raw',badge:r.tag||'\u65e5\u5fd7',title:r.tag?'\u65e5\u5fd7: '+r.tag:'\u539f\u59cb\u65e5\u5fd7',detail:r.msg||r.line,raw:r.line,time:r.time};"
                + "if(low.includes('error')||low.includes('exception')||low.includes('failed')||low.includes('timeout')||low.includes('unable')||low.includes('refused')){e.kind='error';e.state='err';e.badge='\u9519\u8bef';e.title='\u53d1\u73b0\u9519\u8bef\u6216\u5f02\u5e38';return e}"
                + "if(r.tag==='quickjs'){e.kind='quickjs';e.state=r.msg.includes('[error]')?'err':r.msg.includes('[warn]')?'warn':'ok';e.badge='QuickJS';e.title='JS \u5f15\u64ce\u65e5\u5fd7';e.detail=r.msg;return e}"
                + "if(r.tag==='py_spider'){e.kind='py_spider';e.state='ok';e.badge='Python';e.title='Python \u722c\u866b\u65e5\u5fd7';e.detail=r.msg;return e}"
                + "if(r.tag==='okhttp'){e.kind='okhttp';e.state='err';e.badge='\u7f51\u7edc';e.title='HTTP \u8bf7\u6c42\u5f02\u5e38';e.detail=r.msg;return e}"
                + "if(r.tag==='server'){e.kind='server';e.state='raw';e.badge='\u670d\u52a1';e.title='HTTP \u670d\u52a1\u8bf7\u6c42';e.detail=r.msg;return e}"
                + "if(r.tag==='SpiderDebug'){e.kind='SpiderDebug';e.state='raw';e.badge='\u722c\u866b';e.title='\u722c\u866b\u8c03\u8bd5';e.detail=r.msg;return e}"
                + "if(r.tag==='execute_error'){e.kind='error';e.state='err';e.badge='\u5f02\u5e38';e.title='\u6267\u884c\u5f02\u5e38';e.detail=r.msg;return e}"
                + "return e}"
                + "function pass(e,key){const all=(e.raw+' '+e.title+' '+e.detail).toLowerCase();if(key&&!all.includes(key))return false;if(mode==='all')return true;if(mode==='error')return e.kind==='error'||e.state==='err';return e.kind===mode}"
                + "function render(){try{const key=filter.value.trim().toLowerCase();const rows=raw.split('\\n').filter(Boolean).map(parse).map(explain);let shown=0,err=0;const html=[];rows.forEach(e=>{if(e.kind==='error'||e.state==='err')err++;if(!pass(e,key))return;shown++;html.push('<div class=\"entry '+e.state+'\"><div class=\"top\"><span class=\"badge\">'+esc(e.badge)+'</span><span class=\"title\">'+esc(e.title)+'</span><span class=\"time\">'+esc(e.time)+'</span></div><div class=\"detail\">'+esc(e.detail)+'</div><code class=\"rawline\">'+esc(e.raw)+'</code></div>')});logs.innerHTML=html.join('')||'<div class=\"entry raw\"><div class=\"detail\">\u6ca1\u6709\u5339\u914d\u65e5\u5fd7</div></div>';summary.textContent='\u663e\u793a '+shown+'/'+rows.length+' \u884c \u00b7 \u9519\u8bef '+err+' \u6761';rawEl.hidden=true}catch(err){rawEl.hidden=false;logs.innerHTML='<div class=\"entry err\"><div class=\"detail\">\u65e5\u5fd7\u9875\u9762\u6e32\u67d3\u5931\u8d25\uff0c\u5df2\u663e\u793a\u539f\u59cb\u65e5\u5fd7\uff1a'+esc(err&&err.message?err.message:err)+'</div></div>';summary.textContent='\u6e32\u67d3\u5931\u8d25 \u00b7 \u5df2\u663e\u793a\u539f\u59cb\u65e5\u5fd7'}}"
                + "async function poll(){try{if(!paused){const r=await fetch('/debug/stream?v='+lastVersion+'&_='+Date.now(),{cache:'no-store'});const j=await r.json();lastVersion=j.version||lastVersion;meta.textContent=j.size+' \u884c \u00b7 '+Math.ceil((j.bytes||0)/1024)+' KB';if(j.text!==null&&j.text!==undefined){raw=j.text||'';render();if(stick)scrollTo(0,document.body.scrollHeight)}}}catch(e){}setTimeout(poll,1500)}render();poll();";
    }

    private String json(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    private String escape(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.replace("&", "\u0026amp;").replace("<", "\u0026lt;").replace(">", "\u0026gt;").replace("\"", "\u0026quot;").replace("'", "\u0026#39;");
    }
}
