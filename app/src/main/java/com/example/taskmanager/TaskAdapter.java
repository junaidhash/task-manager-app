package com.example.taskmanager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.taskmanager.data.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Task task);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDueDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);

            // Fixed deprecated getAdapterPosition()
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(tasks.get(position));
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    // Add this method to set the click listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (tasks != null && position < tasks.size()) {
                Task task = tasks.get(position);
                holder.tvTitle.setText(task.getTitle());

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                if(task.getDueDate() > 0) {
                    holder.tvDueDate.setText(sdf.format(new Date(task.getDueDate())));
                } else {
                    holder.tvDueDate.setText("No date set");
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("RECYCLERVIEW", "Invalid position: " + position);
        } catch (Exception e) {
            Log.e("BIND_VIEW", "Error binding data: " + e.getMessage());
        }
    }

    public void submitList(List<Task> newTasks) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TaskDiffCallback(tasks, newTasks));
        tasks.clear();
        tasks.addAll(newTasks);
        result.dispatchUpdatesTo(this);
    }

    static class TaskDiffCallback extends DiffUtil.Callback {
        private final List<Task> oldTasks, newTasks;

        TaskDiffCallback(List<Task> oldTasks, List<Task> newTasks) {
            this.oldTasks = oldTasks;
            this.newTasks = newTasks;
        }

        @Override public int getOldListSize() { return oldTasks.size(); }
        @Override public int getNewListSize() { return newTasks.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldTasks.get(oldPos).getId() == newTasks.get(newPos).getId();
        }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldTasks.get(oldPos).equals(newTasks.get(newPos));
        }
    }
}