package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.DialogDetailAllBinding;
import com.fongmi.android.tv.ui.adapter.FlagAllAdapter;
import com.fongmi.android.tv.ui.adapter.PersonAdapter;
import com.fongmi.android.tv.ui.adapter.PersonRowAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.List;

public class DetailAllDialog extends BaseDialog implements FlagAllAdapter.OnClickListener, PersonRowAdapter.OnClickListener {

    private DialogDetailAllBinding binding;
    private FlagAllAdapter mFlagAdapter;
    private PersonRowAdapter mPersonAdapter;
    private OnClickListener mListener;
    private List<Flag> mFlags;
    private List<PersonAdapter.Person> mPersons;
    private int mTitleRes;

    public static DetailAllDialog create() {
        return new DetailAllDialog();
    }

    // 线路模式：弹窗内 2 列网格展示全部线路，标题沿用区块标题（线路）
    public DetailAllDialog flags(List<Flag> items) {
        mFlags = items;
        mTitleRes = R.string.detail_flag;
        return this;
    }

    // 人员模式：弹窗内单列展示导演/演员，item 由主列表的上下结构改为左右行式
    public DetailAllDialog persons(int titleRes, List<PersonAdapter.Person> items) {
        mTitleRes = titleRes;
        mPersons = items;
        return this;
    }

    public DetailAllDialog listener(OnClickListener listener) {
        this.mListener = listener;
        return this;
    }

    public DetailAllDialog show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return this;
        show(manager, tag);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDetailAllBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        // 重建恢复的实例拿不到外部传入的 mTitleRes（=0），setText(0) 会抛 Resources$NotFoundException；
        // BaseDialog 已在重建时拦截并关闭弹窗，此处再兜底一层
        if (mTitleRes != 0) binding.title.setText(mTitleRes);
        binding.list.setItemAnimator(null);
        if (mFlags != null) {
            setFlag();
        } else {
            setPerson();
        }
        limitHeight();
    }

    private void setFlag() {
        // 与选集弹窗一致：chip 自带 6dp 外边距，不再叠加间距装饰；列数按平均名称长度自适应
        binding.list.setLayoutManager(new GridLayoutManager(requireContext(), getSpanCount()));
        binding.list.setAdapter(mFlagAdapter = new FlagAllAdapter(this));
        mFlagAdapter.addAll(mFlags);
        // 初始滚动到当前激活线路，chip 激活态与主列表共用同一 Flag 对象，状态自动同步
        binding.list.scrollToPosition(mFlagAdapter.getPosition());
    }

    // 与 EpisodeGridDialog.setSpanCount 同一套阈值：名称越长列数越少
    private int getSpanCount() {
        if (mFlags.isEmpty()) return 5;
        int total = 0;
        for (Flag item : mFlags) total += item.getShow().length();
        int offset = (int) Math.ceil((double) total / mFlags.size());
        if (offset >= 12) return 1;
        else if (offset >= 8) return 2;
        else if (offset >= 4) return 3;
        else if (offset >= 2) return 4;
        else return 5;
    }

    private void setPerson() {
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.list.setAdapter(mPersonAdapter = new PersonRowAdapter(this));
        mPersonAdapter.addAll(mPersons);
    }

    // 高度跟随内容：线路少时弹窗就矮，不再固定占屏 65%；内容过多时最多占屏 50%，超出部分内部滚动。
    // 限高必须在弹窗展开前完成：展开后再改高度，Behavior 仍按旧高度缓存的偏移定位，弹窗底部会离屏底一段距离
    private void limitHeight() {
        int max = getContentMaxHeight(binding.list, ResUtil.getScreenHeight() * 50 / 100);
        if (binding.list.getMeasuredHeight() <= max) return;
        ViewGroup.LayoutParams params = binding.list.getLayoutParams();
        params.height = max;
        binding.list.setLayoutParams(params);
    }

    // 弹窗内点线路：透传给主页面走与主列表一致的切换逻辑，随后关闭弹窗
    @Override
    public void onItemClick(Flag item) {
        if (mListener != null) mListener.onFlagClick(item);
        dismiss();
    }

    // 弹窗内点导演/演员：与主列表一致跳转搜索该人员作品，随后关闭弹窗
    @Override
    public void onItemClick(Result result) {
        if (mListener != null) mListener.onPersonClick(result);
        dismiss();
    }

    public interface OnClickListener {

        void onFlagClick(Flag item);

        void onPersonClick(Result result);
    }
}
