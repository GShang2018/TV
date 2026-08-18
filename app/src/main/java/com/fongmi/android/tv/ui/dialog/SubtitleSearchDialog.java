package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.SubtitleSearch;
import com.fongmi.android.tv.databinding.DialogSubtitleSearchBinding;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.SubtitleSearchAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class SubtitleSearchDialog extends BaseDialog implements SubtitleSearchAdapter.OnClickListener {

    private final SubtitleSearchAdapter adapter;
    private DialogSubtitleSearchBinding binding;
    private Players player;
    private String name;

    public static SubtitleSearchDialog create() {
        return new SubtitleSearchDialog();
    }

    public SubtitleSearchDialog() {
        this.adapter = new SubtitleSearchAdapter(this);
    }

    public SubtitleSearchDialog player(Players player) {
        this.player = player;
        return this;
    }

    public SubtitleSearchDialog name(String name) {
        this.name = name;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSubtitleSearchBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        if (name != null && !name.isEmpty()) {
            binding.input.setText(name);
            search(name);
        }
    }

    @Override
    protected void initEvent() {
        binding.input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
        binding.search.setOnClickListener(this::doSearch);
    }

    private void doSearch(View... views) {
        String keyword = binding.input.getText().toString().trim();
        if (keyword.isEmpty()) return;
        search(keyword);
    }

    private void search(String keyword) {
        showProgress(true);
        App.execute(() -> {
            try {
                String url = "http://api-shoulei-ssl.xunlei.com/oracle/subtitle?name=" + URLEncoder.encode(keyword, "UTF-8");
                String response = OkHttp.string(url);
                SubtitleSearch result = App.gson().fromJson(response, SubtitleSearch.class);
                App.post(() -> showResult(result));
            } catch (Exception e) {
                App.post(() -> showResult(null));
            }
        });
    }

    private void showResult(SubtitleSearch result) {
        showProgress(false);
        boolean hasData = result != null && result.isSuccess() && !result.getData().isEmpty();
        binding.recycler.setVisibility(hasData ? View.VISIBLE : View.GONE);
        binding.empty.setVisibility(hasData ? View.GONE : View.VISIBLE);
        if (hasData) adapter.setItems(result.getData());
    }

    private void showProgress(boolean show) {
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.search.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(SubtitleSearch.Data item) {
        showProgress(true);
        App.execute(() -> {
            try {
                byte[] bytes = OkHttp.newCall(item.getUrl()).execute().body().bytes();
                // 检测是否为有效 UTF-8，不是则按 GBK 转码
                String text;
                if (isValidUtf8(bytes)) {
                    text = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    text = new String(bytes, "GBK");
                }
                // 保存为本地 UTF-8 文件
                String ext = item.getExt().isEmpty() ? ".srt" : (item.getExt().startsWith(".") ? item.getExt() : "." + item.getExt());
                File file = Path.cache("subtitle_" + System.currentTimeMillis() + ext);
                Path.write(file, text.getBytes(StandardCharsets.UTF_8));
                Sub sub = Sub.from(file.getAbsolutePath());
                App.post(() -> {
                    if (player != null) player.setSub(sub);
                    dismiss();
                });
            } catch (Exception e) {
                App.post(() -> {
                    showProgress(false);
                    if (player != null) player.setSub(item.toSub());
                    dismiss();
                });
            }
        });
    }

    private static boolean isValidUtf8(byte[] bytes) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
