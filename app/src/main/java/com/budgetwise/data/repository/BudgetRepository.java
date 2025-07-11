package com.budgetwise.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.storage.SecurePreferences;
import com.budgetwise.security.EncryptionManager;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {
    private static final String TAG = "BudgetRepository";
    private static final String TRANSACTIONS_KEY = "transactions";
    private static final String BUDGETS_KEY = "budgets";
    
    private final SecurePreferences securePreferences;
    private final ExecutorService executorService;
    
    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Budget>> budgetsLiveData = new MutableLiveData<>();
    
    private List<Transaction> cachedTransactions = new ArrayList<>();
    private List<Budget> cachedBudgets = new ArrayList<>();

    public BudgetRepository(Context context, EncryptionManager encryptionManager) {
        this.securePreferences = new SecurePreferences(context, encryptionManager);
        this.executorService = Executors.newFixedThreadPool(2);
        loadDataFromStorage();
    }

    private void loadDataFromStorage() {
        executorService.execute(() -> {
            try {
                // Load transactions
                Type transactionListType = new TypeToken<List<Transaction>>(){}.getType();
                cachedTransactions = securePreferences.getList(TRANSACTIONS_KEY, transactionListType);
                transactionsLiveData.postValue(new ArrayList<>(cachedTransactions));

                // Load budgets
                Type budgetListType = new TypeToken<List<Budget>>(){}.getType();
                cachedBudgets = securePreferences.getList(BUDGETS_KEY, budgetListType);
                budgetsLiveData.postValue(new ArrayList<>(cachedBudgets));
                
                Log.d(TAG, "Data loaded from storage");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load data from storage", e);
            }
        });
    }

    // Transaction methods
    public LiveData<List<Transaction>> getTransactions() {
        return transactionsLiveData;
    }

    public void addTransaction(Transaction transaction) {
        executorService.execute(() -> {
            cachedTransactions.add(transaction);
            securePreferences.putList(TRANSACTIONS_KEY, cachedTransactions);
            transactionsLiveData.postValue(new ArrayList<>(cachedTransactions));
            updateBudgetSpending(transaction);
        });
    }

    public void updateTransaction(Transaction transaction) {
        executorService.execute(() -> {
            for (int i = 0; i < cachedTransactions.size(); i++) {
                if (cachedTransactions.get(i).getId().equals(transaction.getId())) {
                    cachedTransactions.set(i, transaction);
                    break;
                }
            }
            securePreferences.putList(TRANSACTIONS_KEY, cachedTransactions);
            transactionsLiveData.postValue(new ArrayList<>(cachedTransactions));
        });
    }

    public void deleteTransaction(String transactionId) {
        executorService.execute(() -> {
            cachedTransactions.removeIf(t -> t.getId().equals(transactionId));
            securePreferences.putList(TRANSACTIONS_KEY, cachedTransactions);
            transactionsLiveData.postValue(new ArrayList<>(cachedTransactions));
        });
    }

    // Budget methods
    public LiveData<List<Budget>> getBudgets() {
        return budgetsLiveData;
    }

    public void addBudget(Budget budget) {
        executorService.execute(() -> {
            cachedBudgets.add(budget);
            securePreferences.putList(BUDGETS_KEY, cachedBudgets);
            budgetsLiveData.postValue(new ArrayList<>(cachedBudgets));
        });
    }

    public void updateBudget(Budget budget) {
        executorService.execute(() -> {
            for (int i = 0; i < cachedBudgets.size(); i++) {
                if (cachedBudgets.get(i).getId().equals(budget.getId())) {
                    cachedBudgets.set(i, budget);
                    break;
                }
            }
            securePreferences.putList(BUDGETS_KEY, cachedBudgets);
            budgetsLiveData.postValue(new ArrayList<>(cachedBudgets));
        });
    }

    public void deleteBudget(String budgetId) {
        executorService.execute(() -> {
            cachedBudgets.removeIf(b -> b.getId().equals(budgetId));
            securePreferences.putList(BUDGETS_KEY, cachedBudgets);
            budgetsLiveData.postValue(new ArrayList<>(cachedBudgets));
        });
    }

    private void updateBudgetSpending(Transaction transaction) {
        if (transaction.getType() == Transaction.TransactionType.EXPENSE) {
            for (Budget budget : cachedBudgets) {
                if (budget.getCategory().equals(transaction.getCategory()) && budget.isActive()) {
                    budget.setSpentAmount(budget.getSpentAmount() + transaction.getAmount());
                    updateBudget(budget);
                    break;
                }
            }
        }
    }

    public List<Transaction> getCachedTransactions() {
        return new ArrayList<>(cachedTransactions);
    }

    public List<Budget> getCachedBudgets() {
        return new ArrayList<>(cachedBudgets);
    }
}
