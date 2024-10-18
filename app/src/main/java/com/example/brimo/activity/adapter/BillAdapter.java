package com.example.brimo.activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.example.brimo.activity.BillActivity;
import com.example.brimo.bean.LogBean;
import com.example.brimo.databinding.ItemBillBinding;

import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.ViewHolder> {
    private final List<LogBean> entities;
    private final BillActivity billActivity;

    public BillAdapter(List<LogBean> entities, BillActivity billActivity) {
        this.entities = entities;
        this.billActivity = billActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBillBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemBillBinding binding = ItemBillBinding.bind(holder.itemView);
        LogBean logEntity = entities.get(position);

        binding.time.setText(logEntity.getTime());
        binding.md5.setText(logEntity.getMd5());
        binding.json.setText(JSON.toJSONString(logEntity));
        binding.money.setText(logEntity.getMoney());

        binding.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.more.getRotation() == 0) {
                    binding.more.setRotation(90);
                    binding.moreL.setVisibility(View.VISIBLE);
                } else {
                    binding.more.setRotation(0);
                    binding.moreL.setVisibility(View.GONE);
                }
            }
        });
        binding.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getLayoutPosition();
                notifyItemRemoved(position);
                entities.remove(position);
                billActivity.delete(logEntity.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return entities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
