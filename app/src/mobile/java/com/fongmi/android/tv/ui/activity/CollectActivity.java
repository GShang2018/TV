package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.bean.SuggestTwo;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.SearchAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.PauseExecutor;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class CollectActivity extends BaseActivity implements CustomScroller.Callback, SiteCallback, WordAdapter.OnClickListener, RecordAdapter.OnClickListener, CollectAdapter.OnClickListener, VodAdapter.OnClickListener {

    private ActivityCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private SearchAdapter mSearchAdapter;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private PauseExecutor mExecutor;
    private List<Site> mSites;

    public static void start(Activity activity) {
        start(activity, "");
    }

    public static void start(Activity activity, String keyword) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.putExtra("keyword", keyword);
        activity.startActivity(intent);
    }

    private String getKeyword() {
        return getIntent().getStringExtra("keyword");
    }

    private boolean empty() {
        return mBinding.keyword.getText().toString().trim().isEmpty();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mScroller = new CustomScroller(this);
        mSites = new ArrayList<>();
        setRecyclerView();
        setViewModel();
        checkKeyword();
        setViewType();
        setSite();
        getHot();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.site.setOnClickListener(this::onSite);
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.siteMore.setOnClickListener(this::onSiteMore);
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) search();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
    }

    private void setRecyclerView() {
        // 站点横向标签
        mBinding.siteRecycler.setHasFixedSize(true);
        mBinding.siteRecycler.setItemAnimator(null);
        mBinding.siteRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.siteRecycler.setAdapter(mCollectAdapter = new CollectAdapter(this));
        // 视频网格
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addOnScrollListener(mScroller);
        mBinding.recycler.setAdapter(mSearchAdapter = new SearchAdapter(this));
        // 搜索建议
        mBinding.wordRecycler.setHasFixedSize(false);
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.wordRecycler.setLayoutManager(new FlexboxLayoutManager(this, FlexDirection.ROW));
        // 搜索记录
        mBinding.recordRecycler.setHasFixedSize(false);
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
        mBinding.recordRecycler.setLayoutManager(new FlexboxLayoutManager(this, FlexDirection.ROW));
    }

    private void setViewType() {
        setViewType(Setting.getViewType(ViewType.GRID));
    }

    private void setViewType(int viewType) {
        Setting.putViewType(viewType);
        Style style = viewType == ViewType.PORTRAIT ? new Style("rect", 0.75f) : Style.rect();
        int count = Product.getColumn(this, style);
        mSearchAdapter.setViewType(viewType, count);
        int[] spec = Product.getSpec(this, style);
        mSearchAdapter.setSize(spec);
        ((GridLayoutManager) mBinding.recycler.getLayoutManager()).setSpanCount(count);
        if (viewType == ViewType.PORTRAIT) {
            mBinding.view.setImageResource(R.drawable.ic_action_grid);
        } else {
            mBinding.view.setImageResource(R.drawable.ic_action_portrait);
        }
        // 强制刷新列表
        mSearchAdapter.notifyDataSetChanged();
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> {
            if (mCollectAdapter.getPosition() == 0) mSearchAdapter.addAll(result.getList());
            mCollectAdapter.add(Collect.create(result.getList()));
            mCollectAdapter.add(result.getList());
        });
        mViewModel.result.observe(this, result -> {
            boolean same = result.getList().size() > 0 && mCollectAdapter.getActivated().getSite().equals(result.getList().get(0).getSite());
            if (same) mCollectAdapter.getActivated().getList().addAll(result.getList());
            if (same) mSearchAdapter.addAll(result.getList());
            mScroller.endLoading(result);
        });
    }

    private void checkKeyword() {
        if (TextUtils.isEmpty(getKeyword())) mBinding.keyword.requestFocus();
        else setKeyword(getKeyword());
    }

    private void setKeyword(String text) {
        mBinding.keyword.setText(text);
        mBinding.keyword.setSelection(text.length());
    }

    private void setSite() {
        for (Site site : VodConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
        Site home = VodConfig.get().getHome();
        if (!mSites.contains(home)) return;
        mSites.remove(home);
        mSites.add(0, home);
    }

    private void search() {
        if (empty()) return;
        mSearchAdapter.clear();
        mCollectAdapter.clear();
        Util.hideKeyboard(mBinding.keyword);
        mBinding.site.setVisibility(View.GONE);
        mBinding.agent.setVisibility(View.GONE);
        mBinding.siteLayout.setVisibility(View.VISIBLE);
        mBinding.view.setVisibility(View.VISIBLE);
        mBinding.result.setVisibility(View.VISIBLE);
        if (mExecutor != null) mExecutor.shutdownNow();
        mExecutor = new PauseExecutor(Constant.THREAD_POOL * 2);
        String keyword = mBinding.keyword.getText().toString().trim();
        for (Site site : mSites) mExecutor.execute(() -> search(site, keyword));
        App.post(() -> mRecordAdapter.add(keyword), 250);
        // 更多按钮固定显示，不依赖列表加载完成
        mBinding.siteMore.setVisibility(View.VISIBLE);
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword, false);
        } catch (Throwable ignored) {
        }
    }

    private void getHot() {
        mBinding.word.setText(R.string.search_hot);
        mWordAdapter.addAll(Hot.get(Setting.getHot()));
    }

    private void getSuggest(String text) {
        mBinding.word.setText(R.string.search_suggest);
        mWordAdapter.clear();
        OkHttp.newCall("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box?format=json&page_num=0&page_size=20&key=" + URLEncoder.encode(text)).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (mBinding.keyword.getText().toString().trim().isEmpty()) return;
                List<String> items = SuggestTwo.get(response.body().string());
                App.post(() -> mWordAdapter.appendAll(items));
            }
        });
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + URLEncoder.encode(text)).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (mBinding.keyword.getText().toString().trim().isEmpty()) return;
                List<String> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.appendAll(items), 200);
            }
        });
    }

    private void onSite(View view) {
        Util.hideKeyboard(mBinding.keyword);
        SiteDialog.create(this).search().show();
    }

    private void onSiteMore(View view) {
        showSiteFlexboxDialog(getCollectSites());
    }

    private void showSiteFlexboxDialog(List<Site> sites) {
        if (sites.isEmpty()) return;
        android.widget.LinearLayout layout = (android.widget.LinearLayout) LayoutInflater.from(this).inflate(R.layout.dialog_type, null);
        android.widget.TextView title = layout.findViewById(R.id.title);
        title.setText(R.string.dialog_site_title);
        com.google.android.flexbox.FlexboxLayout flexbox = layout.findViewById(R.id.flexbox);
        flexbox.removeAllViews();
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(layout).create();
        for (int i = 0; i < sites.size(); i++) {
            Site site = sites.get(i);
            android.widget.TextView textView = (android.widget.TextView) LayoutInflater.from(this).inflate(R.layout.adapter_type_dialog, flexbox, false);
            textView.setText(site.getName());
            textView.setSelected(site.isActivated());
            textView.setOnClickListener(v -> {
                setSite(site);
                // 同步更新 CollectAdapter 的选中状态
                for (int j = 0; j < mCollectAdapter.getItemCount(); j++) {
                    Collect collect = mCollectAdapter.getItem(j);
                    if (collect.getSite().getKey().equals(site.getKey())) {
                        mCollectAdapter.setActivated(j);
                        break;
                    }
                }
                dialog.dismiss();
            });
            flexbox.addView(textView);
        }
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private List<Site> getCollectSites() {
        List<Site> sites = new ArrayList<>();
        if (mCollectAdapter == null) return sites;
        for (int i = 0; i < mCollectAdapter.getItemCount(); i++) {
            Collect collect = mCollectAdapter.getItem(i);
            Site site = collect.getSite();
            site.setActivated(collect.isActivated());
            sites.add(site);
        }
        return sites;
    }

    private void syncSiteActivated() {
        // 将 CollectAdapter 的选中状态同步到 mSites
        if (mCollectAdapter == null) return;
        Collect activated = mCollectAdapter.getActivated();
        if (activated == null) return;
        for (Site site : mSites) {
            site.setActivated(site.getKey().equals(activated.getSite().getKey()));
        }
    }

    private void toggleView(View view) {
        setViewType(mSearchAdapter.isGrid() ? ViewType.PORTRAIT : ViewType.GRID);
    }

    private void showAgent() {
        mScroller.reset();
        mSearchAdapter.clear();
        mCollectAdapter.clear();
        mBinding.view.setVisibility(View.GONE);
        mBinding.siteLayout.setVisibility(View.GONE);
        mBinding.result.setVisibility(View.GONE);
        mBinding.site.setVisibility(View.VISIBLE);
        mBinding.agent.setVisibility(View.VISIBLE);
        if (mExecutor != null) mExecutor.shutdownNow();
    }

    private void checkSiteOverflow() {
        if (mCollectAdapter == null || mBinding.siteRecycler.getLayoutManager() == null) return;
        LinearLayoutManager llm = (LinearLayoutManager) mBinding.siteRecycler.getLayoutManager();
        int last = llm.findLastCompletelyVisibleItemPosition();
        int total = mCollectAdapter.getItemCount() - 1;
        mBinding.siteMore.setVisibility(last < total ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setSite(Site item) {
        if (mCollectAdapter == null) return;
        for (int i = 0; i < mCollectAdapter.getItemCount(); i++) {
            Collect collect = mCollectAdapter.getItem(i);
            if (collect.getSite().getKey().equals(item.getKey())) {
                mBinding.recycler.scrollToPosition(0);
                mCollectAdapter.setActivated(i);
                mSearchAdapter.setAll(collect.getList());
                mScroller.setPage(collect.getPage());
                // 横向滚动列表滚动到选中位置
                mBinding.siteRecycler.smoothScrollToPosition(i);
                return;
            }
        }
    }

    @Override
    public void onChanged() {
        mSites.clear();
        setSite();
    }

    @Override
    public void onItemClick(String text) {
        setKeyword(text);
        search();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.record.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        mBinding.recordRecycler.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        App.post(() -> mBinding.recordRecycler.requestLayout(), 250);
    }

    @Override
    public void onItemClick(int position, Collect item) {
        mBinding.recycler.scrollToPosition(0);
        mCollectAdapter.setActivated(position);
        mSearchAdapter.setAll(item.getList());
        mScroller.setPage(item.getPage());
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) FolderActivity.start(this, item.getSiteKey(), Result.folder(item));
        else VideoActivity.collect(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }

    @Override
    public void onLoadMore(String page) {
        Collect activated = mCollectAdapter.getActivated();
        if ("all".equals(activated.getSite().getKey())) return;
        mViewModel.searchContent(activated.getSite(), mBinding.keyword.getText().toString(), page);
        activated.setPage(Integer.parseInt(page));
        mScroller.setLoading(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mExecutor != null) mExecutor.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mExecutor != null) mExecutor.pause();
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.result)) {
            showAgent();
        } else {
            super.onBackPressed();
        }
    }
}
