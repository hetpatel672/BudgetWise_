package com.budgetwise.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.databinding.ItemTransactionBinding;
import com.budgetwise.ui.animations.AnimationUtils;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RecentTransactionAdapter extends ListAdapter<Transaction, RecentTransactionAdapter.TransactionViewHolder> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public RecentTransactionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getAmount() == newItem.getAmount() &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.getCategory().equals(newItem.getCategory()) &&
                   oldItem.getType() == newItem.getType();
        }
    };

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Transaction transaction) {
            // Set transaction icon based on category
            String icon = getTransactionIcon(transaction.getCategory(), transaction.getType());
            binding.iconTransaction.setText(icon);
            
            binding.textDescription.setText(transaction.getDescription());
            binding.textCategory.setText(transaction.getCategory());
            binding.textDate.setText(dateFormat.format(transaction.getDate()));
            binding.textTime.setText(timeFormat.format(transaction.getDate()));
            
            // Format amount based on transaction type
            String amountText;
            int amountColor;
            
            switch (transaction.getType()) {
                case INCOME:
                    amountText = String.format("+$%.2f", transaction.getAmount());
                    amountColor = 0xFF38A169; // Green
                    break;
                case EXPENSE:
                    amountText = String.format("-$%.2f", transaction.getAmount());
                    amountColor = 0xFFE53E3E; // Red
                    break;
                case TRANSFER:
                    amountText = String.format("$%.2f", transaction.getAmount());
                    amountColor = 0xFF4A5568; // Gray
                    break;
                default:
                    amountText = String.format("$%.2f", transaction.getAmount());
                    amountColor = 0xFF4A5568;
                    break;
            }
            
            binding.textAmount.setText(amountText);
            binding.textAmount.setTextColor(amountColor);
            
            // Show recurring indicator with animation
            if (transaction.isRecurring()) {
                binding.iconRecurring.setVisibility(android.view.View.VISIBLE);
                AnimationUtils.pulse(binding.iconRecurring);
            } else {
                binding.iconRecurring.setVisibility(android.view.View.GONE);
            }
            
            // Add click animation
            binding.getRoot().setOnClickListener(v -> AnimationUtils.pulse(v));
        }
        
        private String getTransactionIcon(String category, Transaction.TransactionType type) {
            if (type == Transaction.TransactionType.INCOME) {
                return "💰";
            }
            
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
                default: return "💸";
            }
        }
    }
}
