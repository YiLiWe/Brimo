package com.example.brimo.activity;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.brimo.R;
import com.example.brimo.activity.adapter.BillAdapter;
import com.example.brimo.bean.LogBean;
import com.example.brimo.databinding.ActivityBillBinding;
import com.example.brimo.helper.MyDBOpenHelper;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class BillActivity extends AppCompatActivity {
    private ActivityBillBinding binding;
    private int pageSize = 10, pageNumber = 1;
    private MyDBOpenHelper myDBOpenHelper;
    private final List<LogBean> beans = new ArrayList<>();
    private BillAdapter adapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        myDBOpenHelper = new MyDBOpenHelper(this);
        initRecycler();
        initToolbar();
        initSmart();
        initData();
    }

    private void initSmart() {
        binding.smart.setOnRefreshListener(this::onRefresh);
        binding.smart.setOnLoadMoreListener(this::onLoadMore);
    }

    private void onLoadMore(RefreshLayout refreshLayout) {
        pageNumber = pageNumber + 1;
        initData();
    }

    private void initData() {
        binding.smart.finishLoadMore(0);
        binding.smart.finishRefresh(0);

        int offset = (pageNumber - 1) * pageSize;
        JSONArray data = myDBOpenHelper.getResults("SELECT * FROM log  ORDER BY id DESC LIMIT ? OFFSET ?", new String[]{String.valueOf(pageSize), String.valueOf(offset)});
        for (int i = 0; i < data.size(); i++) {
            JSONObject object = data.getJSONObject(i);
            beans.add(object.to(LogBean.class));
            adapter.notifyItemInserted(beans.size() - 1);
        }
    }

    private void onRefresh(RefreshLayout refreshLayout) {
        adapter.notifyItemRangeRemoved(0, beans.size() + 1);
        beans.clear();
        pageSize = 10;
        pageNumber = 1;
        initData();
    }

    private void initRecycler() {
        adapter = new BillAdapter(beans, this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myDBOpenHelper.close();
    }

    public void delete(long id) {
        SQLiteDatabase db = myDBOpenHelper.getWritableDatabase();
        db.delete("log", "id = ?", new String[]{String.valueOf(id)});
    }

    private void initToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(this::OnMenu);
    }

    private boolean OnMenu(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.delete) {
            SQLiteDatabase db = myDBOpenHelper.getWritableDatabase();
            db.execSQL("DELETE FROM log"); // 假设你的表名为 log
            adapter.notifyItemRangeRemoved(0, beans.size() + 1);
            beans.clear();
        }
        return true;
    }
}
