package com.budgetwise.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.budgetwise.BudgetWiseApplication;
import com.budgetwise.R;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.databinding.DialogAddTransactionBinding;
import com.budgetwise.ai.EnhancedIntelligenceService;
import com.budgetwise.ai.DuplicateGuard;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTransactionDialogFragment extends BottomSheetDialogFragment {
    private static final String ARG_TRANSACTION = "transaction";
    private DialogAddTransactionBinding binding;
    private Transaction editingTransaction;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public static AddTransactionDialogFragment newInstance(@Nullable Transaction transaction) {
        AddTransactionDialogFragment fragment = new AddTransactionDialogFragment();
        Bundle args = new Bundle();
        if (transaction != null) {
            args.putSerializable(ARG_TRANSACTION, transaction);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            editingTransaction = (Transaction) getArguments().getSerializable(ARG_TRANSACTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupSpinners();
        setupDatePicker();
        setupButtons();
        
        if (editingTransaction != null) {
            populateFields();
            binding.textTitle.setText("Edit Transaction");
        } else {
            binding.textTitle.setText("Add Transaction");
        }
    }

    private void setupSpinners() {
        // Transaction type spinner
        String[] types = {"Expense", "Income", "Transfer"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_dropdown_item, types);
        binding.spinnerType.setAdapter(typeAdapter);

        // Category spinner
        String[] categories = {
            "Food & Dining", "Transportation", "Entertainment", "Shopping",
            "Healthcare", "Housing", "Bills & Utilities", "Education",
            "Travel", "Personal Care", "Gifts", "Other"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, categories);
        binding.spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupDatePicker() {
        binding.buttonSelectDate.setText(dateFormat.format(selectedDate.getTime()));
        binding.buttonSelectDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    binding.buttonSelectDate.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void setupButtons() {
        binding.buttonSave.setOnClickListener(v -> saveTransaction());
        binding.buttonCancel.setOnClickListener(v -> dismiss());
        
        // Auto-categorize button
        binding.buttonAutoCategorize.setOnClickListener(v -> {
            String description = binding.editTextDescription.getText().toString().trim();
            if (!description.isEmpty()) {
                EnhancedIntelligenceService intelligenceService = 
                    BudgetWiseApplication.getInstance().getIntelligenceService();
                String suggestedCategory = intelligenceService.categorizeTransaction(description);
                
                // Find and select the category in spinner
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.spinnerCategory.getAdapter();
                int position = adapter.getPosition(suggestedCategory);
                if (position >= 0) {
                    binding.spinnerCategory.setSelection(position);
                    Snackbar.make(binding.getRoot(), "Category suggested: " + suggestedCategory, 
                        Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateFields() {
        if (editingTransaction == null) return;

        binding.editTextDescription.setText(editingTransaction.getDescription());
        binding.editTextAmount.setText(String.valueOf(editingTransaction.getAmount()));
        binding.editTextNotes.setText(editingTransaction.getNotes());
        
        // Set type spinner
        String[] types = {"EXPENSE", "INCOME", "TRANSFER"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(editingTransaction.getType().name())) {
                binding.spinnerType.setSelection(i);
                break;
            }
        }

        // Set category spinner
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.spinnerCategory.getAdapter();
        int position = adapter.getPosition(editingTransaction.getCategory());
        if (position >= 0) {
            binding.spinnerCategory.setSelection(position);
        }

        // Set date
        selectedDate.setTime(editingTransaction.getDate());
        binding.buttonSelectDate.setText(dateFormat.format(selectedDate.getTime()));
        
        binding.switchRecurring.setChecked(editingTransaction.isRecurring());
    }

    private void saveTransaction() {
        if (!validateInput()) return;

        String description = binding.editTextDescription.getText().toString().trim();
        double amount = Double.parseDouble(binding.editTextAmount.getText().toString().trim());
        String category = binding.spinnerCategory.getSelectedItem().toString();
        String notes = binding.editTextNotes.getText().toString().trim();
        boolean isRecurring = binding.switchRecurring.isChecked();

        Transaction.TransactionType type;
        switch (binding.spinnerType.getSelectedItemPosition()) {
            case 1: type = Transaction.TransactionType.INCOME; break;
            case 2: type = Transaction.TransactionType.TRANSFER; break;
            default: type = Transaction.TransactionType.EXPENSE; break;
        }

        // Create transaction object
        Transaction transaction;
        if (editingTransaction != null) {
            // Update existing transaction
            editingTransaction.setDescription(description);
            editingTransaction.setAmount(amount);
            editingTransaction.setCategory(category);
            editingTransaction.setType(type);
            editingTransaction.setDate(selectedDate.getTime());
            editingTransaction.setNotes(notes);
            editingTransaction.setRecurring(isRecurring);
            
            BudgetWiseApplication.getInstance().getBudgetRepository().updateTransaction(editingTransaction);
            dismiss();
        } else {
            // Create new transaction
            transaction = new Transaction(amount, description, category, type);
            transaction.setDate(selectedDate.getTime());
            transaction.setNotes(notes);
            transaction.setRecurring(isRecurring);
            
            // Check for duplicates before adding
            EnhancedIntelligenceService intelligenceService = 
                BudgetWiseApplication.getInstance().getIntelligenceService();
            DuplicateGuard.DuplicateCheckResult duplicateCheck = 
                intelligenceService.checkForDuplicate(transaction);
            
            if (duplicateCheck.isDuplicate() && 
                duplicateCheck.getConfidence() == DuplicateGuard.DuplicateConfidence.HIGH) {
                // Show confirmation dialog for potential duplicate
                showDuplicateConfirmationDialog(transaction, duplicateCheck);
            } else {
                // Add transaction normally
                BudgetWiseApplication.getInstance().getBudgetRepository().addTransaction(transaction);
                
                // Trigger AI analysis
                intelligenceService.runCompleteAnalysis();
                dismiss();
            }
        }
    }
    
    private void showDuplicateConfirmationDialog(Transaction transaction, DuplicateGuard.DuplicateCheckResult duplicateCheck) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Possible Duplicate")
            .setMessage(duplicateCheck.getMessage() + "\n\nDo you want to add this transaction anyway?")
            .setPositiveButton("Add Anyway", (dialog, which) -> {
                BudgetWiseApplication.getInstance().getBudgetRepository().addTransaction(transaction);
                BudgetWiseApplication.getInstance().getIntelligenceService().runCompleteAnalysis();
                dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }


    private boolean validateInput() {
        String description = binding.editTextDescription.getText().toString().trim();
        String amountStr = binding.editTextAmount.getText().toString().trim();

        if (description.isEmpty()) {
            binding.editTextDescription.setError("Description is required");
            return false;
        }

        if (amountStr.isEmpty()) {
            binding.editTextAmount.setError("Amount is required");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.editTextAmount.setError("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            binding.editTextAmount.setError("Invalid amount");
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
