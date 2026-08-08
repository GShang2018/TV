package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.databinding.DialogKeepChooseBinding;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.adapter.KeepChooseAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class KeepChooseDialog extends BaseDialog implements KeepChooseAdapter.OnClickListener {

    private DialogKeepChooseBinding binding;
    private KeepChooseAdapter adapter;
    private Keep keep;
    private OnResultListener listener;
    private String folderName;

    public static KeepChooseDialog create() {
        return new KeepChooseDialog();
    }

    public KeepChooseDialog keep(Keep keep) {
        this.keep = keep;
        return this;
    }

    public KeepChooseDialog listener(OnResultListener listener) {
        this.listener = listener;
        return this;
    }

    public KeepChooseDialog show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogKeepChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
    }

    @Override
    protected void initEvent() {
        binding.create.setOnClickListener(view -> createFolder());
        binding.done.setOnClickListener(view -> onDone());
    }

    private void setRecyclerView() {
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter = new KeepChooseAdapter(this));
        List<KeepFolder> folders = getFolders();
        adapter.addAll(folders);
        // 多选：默认勾选当前所在收藏夹
        adapter.setSelected(Keep.getFolderIds(keep.getKey()));
    }

    private List<KeepFolder> getFolders() {
        List<KeepFolder> folders = new ArrayList<>();
        KeepFolder def = new KeepFolder(getString(R.string.keep_folder_default));
        def.setId(0);
        folders.add(def);
        folders.addAll(KeepFolder.getAll());
        return folders;
    }

    private void createFolder() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null);
        EditText editText = dialogView.findViewById(R.id.editText);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.keep_folder_create)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    String name = editText.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) return;
                    KeepFolder folder = new KeepFolder(name);
                    folder.save();
                    List<KeepFolder> folders = getFolders();
                    adapter.addAll(folders);
                    adapter.setSelected(Keep.getFolderIds(keep.getKey()));
                }).show();
    }

    private void onDone() {
        List<KeepFolder> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) return;
        // 先移出所有收藏夹，再写入勾选的收藏夹
        keep.delete();
        for (KeepFolder folder : selected) {
            keep.setFolderId(folder.getId());
            keep.save();
        }
        folderName = selected.get(0).getName();
        if (listener != null) listener.onResult(selected);
        showResult();
    }

    private void showResult() {
        binding.chooseRoot.setVisibility(View.GONE);
        binding.result.setVisibility(View.VISIBLE);
        binding.resultMessage.setText(getString(R.string.keep_added_to, folderName));
        binding.goSee.setOnClickListener(view -> {
            dismiss();
            KeepActivity.start(requireActivity(), keep.getFolderId());
        });
    }

    @Override
    public void onItemClick(int position) {
        adapter.setSelected(position, !adapter.isSelected(position));
    }

    public interface OnResultListener {
        void onResult(List<KeepFolder> folders);
    }
}
