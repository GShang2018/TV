package com.fongmi.android.tv.ui.dialog;

import android.content.Intent;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.databinding.DialogEpgAllBinding;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.ui.adapter.EpgAllAdapter;
import com.fongmi.android.tv.ui.adapter.EpgDateAdapter;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EpgAllDialog extends BaseDialog implements EpgDateAdapter.OnClickListener, EpgAllAdapter.OnClickListener {

    // 日期窗口：昨天 ~ 未来6天（与 XML 解析保留窗口一致）
    private static final int DAYS_BACK = 1;
    private static final int DAYS_FORWARD = 6;

    private DialogEpgAllBinding binding;
    private EpgDateAdapter mDateAdapter;
    private EpgAllAdapter mEpgAdapter;
    private OnClickListener mListener;
    private LiveViewModel mViewModel;
    private Observer<Epg> mObserver;
    private List<String> mDates;
    private Channel mChannel;
    private String mDate;

    public static EpgAllDialog create() {
        return new EpgAllDialog();
    }

    public EpgAllDialog channel(Channel channel) {
        this.mChannel = channel;
        return this;
    }

    public EpgAllDialog viewModel(LiveViewModel viewModel) {
        this.mViewModel = viewModel;
        return this;
    }

    public EpgAllDialog listener(OnClickListener listener) {
        this.mListener = listener;
        return this;
    }

    public EpgAllDialog show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return this;
        show(manager, tag);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpgAllBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.title.setText(R.string.live_epg_dialog_title);
        binding.date.setHasFixedSize(true);
        binding.date.setItemAnimator(null);
        binding.date.setAdapter(mDateAdapter = new EpgDateAdapter(this));
        binding.list.setHasFixedSize(true);
        binding.list.setItemAnimator(null);
        binding.list.setAdapter(mEpgAdapter = new EpgAllAdapter(this));
        // 传当前频道供节目卡片判断回看/预约状态
        mEpgAdapter.setChannel(mChannel);
        // 节目列表尽量占满弹窗，保证完整节目单可见
        binding.list.getLayoutParams().height = ResUtil.getScreenHeight() * 7 / 10;
        // 横向日期列表：窗口日期 + 频道已缓存日期（如 XML 多天数据），可自由滚动选择
        mDates = buildDates();
        mDateAdapter.addAll(mDates);
        // 先初始化日期，再注册观察者，避免 observeForever 注册时立即回调而 mDate 尚未赋值
        mObserver = this::onEpg;
        setDate(mViewModel.getEpgDate());
        mViewModel.epg.observeForever(mObserver);
        prefetch();
    }

    private void onEpg(Epg epg) {
        // mDate 判空防御：observeForever 注册/弹窗销毁时序下可能先收到旧值
        if (epg == null || mDate == null || !mChannel.getTvgName().equals(epg.getKey())) return;
        if (!mDate.equals(epg.getDate())) return;
        mEpgAdapter.addAll(epg.getList());
        binding.empty.setVisibility(epg.getList().isEmpty() ? View.VISIBLE : View.GONE);
    }

    // 自动预取窗口内全部日期的节目单，切换日期直接读缓存；XML 多天数据已由解析写入缓存，无需请求
    private void prefetch() {
        if (mChannel.getEpg().isEmpty()) return;
        List<String> order = new ArrayList<>(mDates);
        order.remove(mDate);
        order.add(0, mDate);
        mViewModel.getEpgDates(mChannel, order);
    }

    // 日期横滑点击：跳转到指定日期
    @Override
    public void onItemClick(String date) {
        setDate(date);
    }

    // 时间线点击：转发给播放页处理（已结束且支持回放时播放）
    @Override
    public void onItemClick(EpgData item) {
        if (mListener != null) mListener.onItemClick(item);
        dismiss();
    }

    // 预约：调起系统日历新建事件提醒（预填节目标题与时间，由用户在日历中确认保存）
    @Override
    public void onReserve(EpgData item) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.Events.TITLE, item.getTitle());
            intent.putExtra(CalendarContract.Events.DESCRIPTION, mChannel.getName());
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.getStartTime());
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.getEndTime());
            requireContext().startActivity(intent);
            Notify.show(getString(R.string.live_epg_reserve_toast));
        } catch (Exception e) {
            Notify.show(getString(R.string.live_epg_reserve_fail));
        }
    }

    private void setDate(String date) {
        if (mDate != null && mDate.equals(date)) return;
        mDate = date;
        int position = Math.max(mDates.indexOf(date), 0);
        mDateAdapter.setSelected(position);
        binding.date.scrollToPosition(position);
        // 缓存命中直接展示；未命中清空占位，由预取/单次请求回调填充
        Epg data = mChannel.findData(date);
        if (data != null && !data.getList().isEmpty()) {
            mEpgAdapter.addAll(data.getList());
            binding.empty.setVisibility(View.GONE);
        } else {
            mEpgAdapter.clear();
            binding.empty.setVisibility(View.VISIBLE);
            // 预取尚未到达或失败时单发请求兜底
            if (!mChannel.getEpg().isEmpty()) mViewModel.getEpg(mChannel, date);
        }
    }

    // 日期窗口：昨天 ~ 未来6天，并合并频道缓存中已有日期（如 XML 多天节目单），排序后供横向列表展示
    private List<String> buildDates() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -DAYS_BACK);
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < DAYS_BACK + DAYS_FORWARD + 1; i++) {
            dates.add(format.format(calendar.getTime()));
            calendar.add(Calendar.DATE, 1);
        }
        for (String date : mChannel.getDateKeys()) if (!dates.contains(date)) dates.add(date);
        Collections.sort(dates);
        return dates;
    }

    @Override
    public void onDestroy() {
        if (mViewModel != null) mViewModel.epg.removeObserver(mObserver);
        super.onDestroy();
    }

    public interface OnClickListener {

        void onItemClick(EpgData item);
    }
}
