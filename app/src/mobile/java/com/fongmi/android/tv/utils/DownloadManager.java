package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.event.DownloadEvent;
import com.fongmi.android.tv.service.DownloadService;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {

    private static final int MAX_CONCURRENT = 2;
    private static final int PROGRESS_INTERVAL = 500;

    private final ExecutorService executor;
    private final Map<String, Task> tasks;
    private final Map<String, Future<?>> running;
    private final LinkedBlockingQueue<Task> queue;
    private volatile boolean recovered;

    private static class Loader {
        static final DownloadManager INSTANCE = new DownloadManager();
    }

    public static DownloadManager get() {
        Loader.INSTANCE.recover();
        return Loader.INSTANCE;
    }

    private DownloadManager() {
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        this.tasks = new ConcurrentHashMap<>();
        this.running = new ConcurrentHashMap<>();
        this.queue = new LinkedBlockingQueue<>();
    }

    private static class Task {

        final Download item;
        final AtomicBoolean paused = new AtomicBoolean(false);
        final AtomicBoolean cancelled = new AtomicBoolean(false);

        Task(Download item) {
            this.item = item;
        }
    }

    private static class CancelledException extends Exception {
    }

    private void recover() {
        if (recovered) return;
        recovered = true;
        // 进程被杀后残留的"下载中"记录改成暂停，避免界面显示成卡住的下载中
        try {
            for (Download item : Download.get()) {
                if (item.getStatus() == Download.STATUS_DOWNLOADING) {
                    item.setStatus(Download.STATUS_PAUSED);
                    item.save();
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void enqueue(Download item) {
        if (TextUtils.isEmpty(item.getSavePath())) item.setSavePath(new File(getDownloadDir(), item.getFileName()).getAbsolutePath());
        Task existing = tasks.get(item.getId());
        if (existing != null) {
            int status = existing.item.getStatus();
            if (status == Download.STATUS_QUEUED || status == Download.STATUS_DOWNLOADING || status == Download.STATUS_PAUSED) return;
        }
        item.setStatus(Download.STATUS_QUEUED);
        item.setErrorMsg("");
        item.save();
        Task task = new Task(item);
        tasks.put(item.getId(), task);
        queue.offer(task);
        DownloadEvent.add(item);
        DownloadService.start(App.get());
        scheduleNext();
    }

    public void pause(String id) {
        Task task = tasks.get(id);
        if (task == null) return;
        Download item = task.item;
        if (item.isFinished()) return;
        task.paused.set(true);
        synchronized (task) {
            task.notifyAll();
        }
        item.setStatus(Download.STATUS_PAUSED);
        item.save();
        DownloadEvent.update(item);
    }

    public void resume(String id) {
        Task task = tasks.get(id);
        if (task == null) return;
        task.paused.set(false);
        synchronized (task) {
            task.notifyAll();
        }
        if (!running.containsKey(id)) queue.offer(task);
        Download item = task.item;
        item.setStatus(Download.STATUS_QUEUED);
        item.save();
        DownloadEvent.update(item);
        DownloadService.start(App.get());
        scheduleNext();
    }

    public void retry(String id) {
        Download item = Download.find(id);
        if (item == null) return;
        item.setStatus(Download.STATUS_QUEUED);
        item.setProgress(0);
        item.setTotal(-1);
        item.setErrorMsg("");
        item.save();
        Task task = tasks.get(id);
        if (task == null) {
            task = new Task(item);
            tasks.put(id, task);
        }
        task.cancelled.set(false);
        task.paused.set(false);
        queue.offer(task);
        DownloadEvent.update(item);
        DownloadService.start(App.get());
        scheduleNext();
    }

    public void delete(String id) {
        Task task = tasks.remove(id);
        if (task != null) {
            task.cancelled.set(true);
            task.paused.set(false);
            synchronized (task) {
                task.notifyAll();
            }
            running.remove(id);
        }
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            if (it.next().item.getId().equals(id)) it.remove();
        }
        Download item = Download.find(id);
        if (item != null) {
            new File(item.getSavePath() + ".part").delete();
            new File(item.getSavePath()).delete();
            item.delete();
            DownloadEvent.delete(item);
        }
        if (tasks.isEmpty()) DownloadService.stop(App.get());
    }

    public void pauseAll() {
        for (Task task : tasks.values()) {
            int status = task.item.getStatus();
            if (status == Download.STATUS_QUEUED || status == Download.STATUS_DOWNLOADING) pause(task.item.getId());
        }
    }

    public void resumeAll() {
        for (Task task : tasks.values()) if (task.paused.get()) resume(task.item.getId());
    }

    public boolean hasActive() {
        for (Task task : tasks.values()) {
            int status = task.item.getStatus();
            if (status == Download.STATUS_QUEUED || status == Download.STATUS_DOWNLOADING || status == Download.STATUS_PAUSED) return true;
        }
        return false;
    }

    private void scheduleNext() {
        while (running.size() < MAX_CONCURRENT) {
            Task task = queue.poll();
            if (task == null) break;
            if (task.cancelled.get() || task.paused.get()) continue;
            Future<?> future = executor.submit(() -> run(task));
            running.put(task.item.getId(), future);
        }
    }

    private void run(Task task) {
        Download item = task.item;
        try {
            item.setStatus(Download.STATUS_DOWNLOADING);
            item.save();
            DownloadEvent.update(item);
            doDownload(task);
            item.setStatus(Download.STATUS_FINISHED);
            item.setErrorMsg("");
            item.save();
            DownloadEvent.update(item);
        } catch (CancelledException e) {
            // 已取消，记录与文件在 delete() 中清理
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (task.cancelled.get()) return;
            item.setStatus(Download.STATUS_FAILED);
            item.setErrorMsg(e.getMessage());
            item.save();
            DownloadEvent.update(item);
        } finally {
            running.remove(item.getId());
            scheduleNext();
        }
    }

    private void doDownload(Task task) throws Exception {
        Download item = task.item;
        File part = new File(item.getSavePath() + ".part");
        long downloaded = part.exists() ? part.length() : 0;
        item.setProgress(downloaded);

        Request.Builder builder = new Request.Builder().url(item.getUrl());
        if (downloaded > 0) builder.header("Range", "bytes=" + downloaded + "-");
        Headers headers = parseHeaders(item.getHeader());
        if (headers != null) builder.headers(headers);

        try (Response response = OkHttp.client().newCall(builder.build()).execute()) {
            long total = item.getTotal();
            if (response.code() == 206) {
                total = downloaded + length(response.header("Content-Length"));
            } else if (response.code() == 200) {
                if (downloaded > 0) {
                    part.delete();
                    downloaded = 0;
                }
                total = length(response.header("Content-Length"));
            } else {
                throw new java.io.IOException("HTTP " + response.code());
            }
            item.setTotal(total);
            if (isM3u8(item.getUrl(), response.header("Content-Type"))) {
                item.setType(1);
                throw new java.io.IOException(ResUtil.getString(R.string.download_m3u8_unsupported));
            }
            try (InputStream input = response.body().byteStream(); FileOutputStream output = new FileOutputStream(part, downloaded > 0)) {
                byte[] buffer = new byte[8192];
                long lastPost = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    synchronized (task) {
                        boolean wasPaused = task.paused.get();
                        while (task.paused.get() && !task.cancelled.get()) task.wait();
                        if (wasPaused && !task.paused.get()) item.setStatus(Download.STATUS_DOWNLOADING);
                    }
                    if (task.cancelled.get()) throw new CancelledException();
                    output.write(buffer, 0, read);
                    downloaded += read;
                    item.setProgress(downloaded);
                    long now = System.currentTimeMillis();
                    if (now - lastPost >= PROGRESS_INTERVAL) {
                        lastPost = now;
                        item.save();
                        DownloadEvent.update(item);
                    }
                }
            }
            item.setProgress(downloaded);
            item.setTotal(total);
            item.save();
            File target = new File(item.getSavePath());
            if (target.exists()) target.delete();
            if (!part.renameTo(target)) throw new java.io.IOException("rename failed");
        }
    }

    private boolean isM3u8(String url, String contentType) {
        String lower = url == null ? "" : url.toLowerCase();
        if (lower.contains(".m3u8") || lower.contains(".m3u")) return true;
        String type = contentType == null ? "" : contentType.toLowerCase();
        return type.contains("mpegurl") || type.contains("m3u8");
    }

    private long length(String header) {
        if (TextUtils.isEmpty(header)) return -1;
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Headers parseHeaders(String json) {
        if (TextUtils.isEmpty(json)) return null;
        try {
            Map<String, String> map = App.gson().fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
            return map == null || map.isEmpty() ? null : Headers.of(map);
        } catch (Exception e) {
            return null;
        }
    }

    private static File getDownloadDir() {
        File dir = new File(Path.tv(), "download");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
}
