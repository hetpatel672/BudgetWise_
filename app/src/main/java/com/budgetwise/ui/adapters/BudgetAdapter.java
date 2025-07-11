package com.budgetwise.ui.adapters;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetwise.data.models.Budget;
import com.budgetwise.databinding.ItemBudgetBinding;
import com.budgetwise.ui.animations.AnimationUtils;

public class BudgetAdapter extends ListAdapter<Budget, BudgetAdapter.BudgetViewHolder> {

    public BudgetAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Budget> DIFF_CALLBACK = new DiffUtil.ItemCallback<Budget>() {
        @Override
        public boolean areItemsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
            return oldItem.getBudgetAmount() == newItem.getBudgetAmount() &&
                   oldItem.getSpentAmount() == newItem.getSpentAmount() &&
                   oldItem.getCategory().equals(newItem.getCategory());
        }
    };

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBudgetBinding binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new BudgetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final ItemBudgetBinding binding;

        BudgetViewHolder(ItemBudgetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Budget budget) {
            // Set category icon based on category
            String icon = getCategoryIcon(budget.getCategory());
            binding.iconCategory.setText(icon);
            
            binding.textCategory.setText(budget.getCategory());
            binding.textBudgetAmount.setText(String.format("$%.2f", budget.getBudgetAmount()));
            binding.textSpentAmount.setText(String.format("$%.2f", budget.getSpentAmount()));
            binding.textRemainingAmount.setText(String.format("$%.2f", budget.getRemainingAmount()));
            binding.textBudgetPercentage.setText(String.format("%.0f%% used", budget.getSpentPercentage()));
            
            // Update progress bar
            int progress = (int) budget.getSpentPercentage();
            AnimationUtils.animateProgress(binding.progressBudget, 0, Math.min(progress, 100));
            
            // Change color if over budget
            if (budget.isOverBudget()) {
                binding.progressBudget.setIndicatorColor(0xFFE53E3E);
                binding.textSpentAmount.setTextColor(0xFFE53E3E);
                binding.textBudgetPercentage.setTextColor(0xFFE53E3E);
                
                // Shake animation for over budget
                AnimationUtils.shake(binding.getRoot());
            } else {
                binding.progressBudget.setIndicatorColor(0xFF38A169);
                binding.textSpentAmount.setTextColor(0xFF4A5568);
                binding.textBudgetPercentage.setTextColor(0xFF4A5568);
            }
            
            // Add click animation
            binding.getRoot().setOnClickListener(v -> AnimationUtils.pulse(v));
        }
        
        private String getCategoryIcon(String category) {
            switch (category.toLowerCase()) {
                case "food & dining": return "🍽️";
                case "transportation": return "🚗";
                case "entertainment": return "🎬";
                case "shopping": return "🛒";
                case "healthcare": return "🏥";
                case "housing": return "🏠";
                case "bills & utilities": return "💡";
                case "education": return "📚";
                case "travel": return "✈️";
                case "personal care": return "💄";
                case "gifts": return "🎁";
                default: return "💳";
            }
        }
    }
}
