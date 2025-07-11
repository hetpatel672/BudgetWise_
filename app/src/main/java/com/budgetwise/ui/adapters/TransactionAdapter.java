package com.budgetwise.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.databinding.ItemTransactionBinding;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final OnTransactionClickListener clickListener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
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
                   oldItem.getType() == newItem.getType() &&
                   oldItem.getUpdatedAt() == newItem.getUpdatedAt();
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

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onTransactionClick(getItem(position));
                }
            });
        }

        void bind(Transaction transaction) {
            binding.textDescription.setText(transaction.getDescription());
            binding.textCategory.setText(transaction.getCategory());
            binding.textDate.setText(dateFormat.format(transaction.getDate()));
            
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
            
            // Show recurring indicator
            binding.iconRecurring.setVisibility(
                transaction.isRecurring() ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }
}
