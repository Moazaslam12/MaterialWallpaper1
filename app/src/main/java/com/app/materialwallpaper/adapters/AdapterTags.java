package com.app.materialwallpaper.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.materialwallpaper.R;
import com.app.materialwallpaper.activities.ActivitySearch;

import java.util.ArrayList;

public class AdapterTags extends RecyclerView.Adapter<AdapterTags.ViewHolder> {

    ArrayList<String> arrayList;
    Context context;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView txt_tags;

        public ViewHolder(View view) {
            super(view);
            txt_tags = view.findViewById(R.id.item_tags);
        }

    }

    public AdapterTags(Context context, ArrayList<String> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tags, parent, false);
        return new ViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        holder.txt_tags.setText(arrayList.get(position).toLowerCase());

        holder.txt_tags.setOnClickListener(view -> {
            Intent intent = new Intent(context, ActivitySearch.class);
            intent.putExtra("tags", arrayList.get(position).toLowerCase());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}