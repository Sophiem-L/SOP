package com.knowledgebase.sopviewer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private List<FolderDoc> folders;
    private Context context;

    public FolderAdapter(List<FolderDoc> folders, Context context) {
        this.folders = folders;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderDoc folder = folders.get(position);
        holder.title.setText(folder.getTitle());
        holder.count.setText(folder.getDocCount() + " Documents");
        holder.date.setText("Last edited on " + folder.getLastEdited());

        // Dynamically tint the background
        holder.container
                .setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, folder.getColorResId())));
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, count, date;
        View container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            title = itemView.findViewById(R.id.folderName);
            count = itemView.findViewById(R.id.docCount);
            date = itemView.findViewById(R.id.lastEdited);
        }
    }
}
