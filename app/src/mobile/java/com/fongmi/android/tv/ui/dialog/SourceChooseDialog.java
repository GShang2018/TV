package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.DialogSourceChooseBinding;
import com.fongmi.android.tv.ui.adapter.SourceChooseAdapter;
import com.fongmi.android.tv.ui.fragment.SourceFragment;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 播放源弹窗：按站源分组，一个站点一个 tab（样式对齐选集弹窗 EpisodeGridDialog）。
 * 检索结果会持续增量回填，所以每次 refresh 都要重算分组并把新数据推给各页。
 */
public class SourceChooseDialog extends BaseDialog implements SourceChooseAdapter.OnClickListener {

    private DialogSourceChooseBinding binding;
    private OnClickListener listener;
    private final List<Vod> items = new ArrayList<>();        // 扁平全量结果（外部列表的副本）
    private final List<String> sites = new ArrayList<>();      // 站点名，按首次出现顺序
    private final Map<String, List<Vod>> groups = new LinkedHashMap<>();
    private final List<String> searchableBefore = new ArrayList<>(); // 打开筛选弹窗前的可检索站点，用于判断开关是否真的变了
    private FragmentManager.FragmentLifecycleCallbacks filterCallbacks;
    private int listHeight;                                    // 列表可视高度（可用最大高度，超出一屏时内部滚动）
    private Vod selected;                                      // 当前正在播放的源
    private String keyword;                                    // 检索关键词（宿主预填当前片名，可在弹窗内改）
    private boolean quick = true;                               // 快速搜索开关：对应 spider/CMS 的 quick 参数，默认开（与原行为一致）
    private String viewingSite;                                // 当前展示的 tab 对应站点（重排后用它把视图钉回原处）
    private boolean searching = true;                          // 检索未结束（finish() 置 false）
    private boolean located;                                   // 选中项所在 tab 只自动定位一次
    private boolean userMoved;                                 // 用户手动切过 tab 之后不再自动定位
    private boolean suppressLocate;                            // 屏蔽程序化选中触发的回调

    public static SourceChooseDialog create() {
        return new SourceChooseDialog();
    }

    public SourceChooseDialog items(List<Vod> items) {
        this.items.clear();
        if (items != null) this.items.addAll(items);
        return this;
    }

    public SourceChooseDialog selected(int selected) {
        // 保持宿主原有语义：传入的是扁平列表里的下标，这里立刻换算成对象（按站点分页后下标不再通用）
        this.selected = selected >= 0 && selected < items.size() ? items.get(selected) : null;
        return this;
    }

    public SourceChooseDialog listener(OnClickListener listener) {
        this.listener = listener;
        return this;
    }

    public SourceChooseDialog keyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public SourceChooseDialog show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return this;
        show(manager, tag);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSourceChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setPager();
        setSearch();
        setFilter();
        setListHeight();
        rebuild();
    }

    // 关键词输入框：预填宿主传进来的当前片名（不自动聚焦，避免打开弹窗就弹键盘挡住列表）
    private void setSearch() {
        binding.keyword.setText(keyword == null ? "" : keyword);
        binding.keyword.setSelection(binding.keyword.getText().length());
        binding.keyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                search();
                return true;
            }
            return false;
        });
        binding.searchBtn.setOnClickListener(v -> search());
        // 快速搜索开关：开 = quick=true，关 = quick=false（对应 spider/CMS 的 quick 参数）。
        // 先设初值再挂监听，避免初始化时被当成一次切换而触发多余检索。
        binding.quickSwitch.setChecked(quick);
        binding.quickSwitch.setOnCheckedChangeListener((v, checked) -> {
            quick = checked;
            // 开关本身就是在换检索口径，切换后用当前关键词立刻重搜一次（不收键盘，可能还在输入）
            String text = binding.keyword.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) restart(text);
        });
    }

    // 点搜索按钮 / 键盘搜索键：收起键盘后按当前关键词重新检索
    private void search() {
        if (binding == null || listener == null) return;
        String text = binding.keyword.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return; // 空关键词不发检索，否则各站点会吐一堆无关结果
        Util.hideKeyboard(binding.keyword);
        binding.keyword.clearFocus();
        restart(text);
    }

    // 回到加载态并让宿主按"当前关键词 + 当前快速搜索开关"重跑跨站检索
    private void restart(String text) {
        if (binding == null || listener == null) return;
        keyword = text;
        resetState();
        listener.onSearch(text, quick);
    }

    // 站点筛选入口（搜索行右侧）：与首页搜索页同一个开关，改完重新检索站点集合
    private void setFilter() {
        binding.filter.setOnClickListener(v -> {
            searchableBefore.clear();
            for (Site site : VodConfig.get().getSites()) if (site.isSearchable()) searchableBefore.add(site.getKey());
            SiteDialog.create().search().show(SourceChooseDialog.this);
        });
        filterCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                if (fragment instanceof SiteDialog) filterChanged();
            }
        };
        // 只监听本弹窗的直接子 Fragment：筛选弹窗挂在子 FragmentManager 上（Fragment 重载不会被"已有 BottomSheet 就不弹"的守卫挡掉，也会叠在本弹窗之上）
        getChildFragmentManager().registerFragmentLifecycleCallbacks(filterCallbacks, false);
    }

    // 筛选变化：先回到加载态并通知宿主重新检索（筛选只影响下一次检索的站点集合）
    private void filterChanged() {
        if (binding == null) return;
        // 只是关掉筛选弹窗、开关没动时不重跑检索，避免白白重搜一轮
        List<String> after = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) if (site.isSearchable()) after.add(site.getKey());
        if (after.equals(searchableBefore)) return;
        if (listener == null) return;
        resetState();
        listener.onFilterChanged();
    }

    // 重新检索前回到加载态：旧结果与分组全部丢弃，定位标记复位（关键词/筛选变化共用）
    private void resetState() {
        items.clear();
        groups.clear();
        sites.clear();
        viewingSite = null;
        searching = true;
        located = false;
        userMoved = false;
        // 手动联动后标签要自己清；同时必须通知 pager 清空，否则 ViewPager2 仍按旧 count 要页面会越界
        if (binding.tabs.getTabCount() > 0) binding.tabs.removeAllTabs();
        if (binding.pager.getAdapter() != null) binding.pager.getAdapter().notifyDataSetChanged();
        rebuild();
    }

    // 列表高度：直接按可用最大高度铺满（屏幕高度扣掉拖动条/标题/搜索行/tab 等固定部分），
    // 条数超过一屏时由列表内部滚动。必须在弹窗展开前算好并下发给各页：
    // 展开后再改高度，Behavior 会按旧高度缓存的偏移定位
    private void setListHeight() {
        listHeight = getContentMaxHeight(binding.pager, ResUtil.getScreenHeight() - ResUtil.dp2px(96));
    }

    private void setPager() {
        binding.pager.setAdapter(new PageAdapter(this));
        // 手写 tab ↔ pager 联动，替代 TabLayoutMediator：mediator 对任何数据变化（含 onItemRangeInserted）
        // 都会走 populateTabsFromPagerAdapter() 全量重建标签，做不到"搜出一个站点就只追加一个 tab"。
        // 改成手动管理后，新站点到达时只 addTab 一个，已有标签与选中态完全不受影响。
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 程序化同步（新增标签被自动选中、定位时切页）触发的回调不算"用户自己切了 tab"
                if (suppressLocate) return;
                userMoved = true;
                binding.pager.setCurrentItem(tab.getPosition(), true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position >= 0 && position < sites.size()) viewingSite = sites.get(position);
                TabLayout.Tab tab = binding.tabs.getTabAt(position);
                if (tab == null || tab.isSelected()) return;
                suppressLocate = true;
                tab.select();
                suppressLocate = false;
            }
        });
    }

    /**
     * 检索结果实时回填：更新全量数据与选中源，重算站点分组。
     * 有数据时关闭加载动画；空结果保持加载态，由 finish() 在检索结束时统一关闭。
     */
    public void refresh(List<Vod> items, int selected) {
        if (binding == null) return;
        this.items.clear();
        if (items != null) this.items.addAll(items);
        // 选中源所在站点还没回结果时 (selected = -1) 保留上一次的选中项，等后续 refresh 再定位
        if (selected >= 0 && selected < this.items.size()) this.selected = this.items.get(selected);
        rebuild();
    }

    /**
     * 检索结束：关闭加载动画（空态提示由布局展示）。
     * 由 VideoActivity 在静默期结束 / 兜底超时时调用。
     */
    public void finish() {
        if (!isVisible()) return;
        searching = false;
        setState();
    }

    private void rebuild() {
        String before = viewingSite;
        // 各站点分组重算：同一站点的结果会继续增长，重算后统一推给对应页面
        Map<String, List<Vod>> next = new LinkedHashMap<>();
        for (Vod item : items) next.computeIfAbsent(siteOf(item), k -> new ArrayList<>()).add(item);
        groups.clear();
        groups.putAll(next);
        // 本次新搜出结果的站点（按首次出现顺序）；当前播放源所在站点提到最前，稍后插到第一个 tab
        List<String> pending = new ArrayList<>();
        for (String site : groups.keySet()) if (!sites.contains(site)) pending.add(site);
        String current = selected == null ? null : siteOf(selected);
        if (current != null && pending.remove(current)) pending.add(0, current);
        // 逐条插入：sites、pager 的插入通知、tab 三者必须在同一节奏上推进。
        // 不能"先把新站点全塞进 sites、再按最终位置逐个通知"——那时 index 是最终位置，
        // 而 TabLayout / RecyclerView 还停在旧数量，只要有一个站点插到第 0 位（其余站点索引整体后移），
        // 插入到中间位置就会 IndexOutOfBoundsException（Index: 1, Size: 0）。
        if (binding.pager.getAdapter() != null) {
            for (String site : pending) {
                // 只有"当前播放源所在站点"插到最前（它一定是 pending 的第一个），其余一律追加到末尾
                int index = site.equals(current) ? 0 : sites.size();
                sites.add(index, site);
                binding.pager.getAdapter().notifyItemInserted(index);
                suppressLocate = true;
                binding.tabs.addTab(binding.tabs.newTab().setText(site), index);
                suppressLocate = false;
            }
        }
        // 先切状态再定位：pager 变为可见后再 setCurrentItem，避免在 GONE 状态下设置当前页
        setState();
        pushData();
        if (located || userMoved) reattach(before);
        else locate();
        // 新标签插入（尤其插到第 0 位）后 TabLayout 的选中位置可能与 pager 当前页脱节，兜底对齐一次
        syncTabToPager();
        updateTitle();
    }

    // 让标签选中态跟住 pager 当前页（位置没变时 setCurrentItem 不会回调，得主动 select）
    private void syncTabToPager() {
        TabLayout.Tab tab = binding.tabs.getTabAt(Math.min(binding.pager.getCurrentItem(), Math.max(sites.size() - 1, 0)));
        if (tab == null || tab.isSelected()) return;
        suppressLocate = true;
        tab.select();
        suppressLocate = false;
    }

    // tab 重排后把展示位置钉回原来那个站点，避免用户正看着的列表被换掉
    private void reattach(String site) {
        if (site == null) return;
        int index = sites.indexOf(site);
        if (index < 0 || binding.pager.getCurrentItem() == index) return;
        suppressLocate = true;
        binding.pager.setCurrentItem(index, false);
        binding.tabs.post(() -> suppressLocate = false);
    }

    // 站点标识：优先用站点显示名，为空时退回站点 key（tab 文案与分组都必须走这里，保证一致）
    private String siteOf(Vod item) {
        return TextUtils.isEmpty(item.getSiteName()) ? item.getSiteKey() : item.getSiteName();
    }

    // 把最新数据推给已创建的页（新页由 createFragment 自己取）
    private void pushData() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (!(fragment instanceof SourceFragment)) continue;
            SourceFragment source = (SourceFragment) fragment;
            List<Vod> group = groups.get(source.getSite());
            source.apply(group == null ? new ArrayList<>() : new ArrayList<>(group), selected);
        }
    }

    // 打开弹窗后只自动定位一次：选中源所在站点还没有结果时等下一次 refresh
    private void locate() {
        if (located || userMoved || selected == null) return;
        int index = sites.indexOf(siteOf(selected));
        if (index < 0) return;
        suppressLocate = true;
        binding.pager.setCurrentItem(index, false);
        binding.tabs.post(() -> suppressLocate = false);
        located = true;
    }

    private void setState() {
        boolean hasData = !items.isEmpty();
        binding.tabs.setVisibility(hasData ? View.VISIBLE : View.GONE);
        binding.pager.setVisibility(hasData ? View.VISIBLE : View.GONE);
        binding.progress.setVisibility(!hasData && searching ? View.VISIBLE : View.GONE);
        binding.empty.setVisibility(!hasData && !searching ? View.VISIBLE : View.GONE);
    }

    private void updateTitle() {
        int count = items.size();
        if (count > 0) binding.title.setText(getString(R.string.dialog_source_count, count));
        else binding.title.setText(R.string.dialog_source_choose);
    }

    @Override
    public void onItemClick(Vod item) {
        if (listener != null) listener.onItemClick(item);
        dismiss();
    }

    // 拖动关闭时反注册筛选监听
    @Override
    public void onDestroyView() {
        if (filterCallbacks != null) {
            getChildFragmentManager().unregisterFragmentLifecycleCallbacks(filterCallbacks);
            filterCallbacks = null;
        }
        super.onDestroyView();
    }

    class PageAdapter extends FragmentStateAdapter {

        public PageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String site = sites.get(position);
            SourceFragment fragment = SourceFragment.newInstance(site);
            fragment.setListener(SourceChooseDialog.this);
            fragment.setListHeight(listHeight);
            List<Vod> group = groups.get(site);
            fragment.apply(group == null ? new ArrayList<>() : new ArrayList<>(group), selected);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return sites.size();
        }

        // 用站点名作为稳定 id：新站点追加时不会让已有页被重建
        @Override
        public long getItemId(int position) {
            return sites.get(position).hashCode();
        }

        @Override
        public boolean containsItem(long itemId) {
            for (String site : sites) if (site.hashCode() == itemId) return true;
            return false;
        }
    }

    public interface OnClickListener {

        void onItemClick(Vod item);

        // 站点筛选发生变化：宿主需要按新的站点集合重新检索
        default void onFilterChanged() {
        }

        // 关键词被改过或快速搜索开关被切换：宿主按新关键词重新检索（quick 对应 spider/CMS 的 quick 参数）
        default void onSearch(String keyword, boolean quick) {
        }
    }
}
