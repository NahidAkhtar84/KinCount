package com.nahid.kincount.data.local

import com.nahid.kincount.data.model.Category
import com.nahid.kincount.data.model.TxType

/** Default categories seeded into every fresh install (PLAN.md §7). */
object CategorySeed {

    val defaults: List<Category> = listOf(
        // Expense
        Category("cat_food", "Food & Dining", TxType.EXPENSE, "🍔", true, 10),
        Category("cat_groceries", "Groceries", TxType.EXPENSE, "🍒", true, 20),
        Category("cat_transport", "Transport", TxType.EXPENSE, "🚌", true, 30),
        Category("cat_fuel", "Fuel", TxType.EXPENSE, "⛽", true, 40),
        Category("cat_home", "Rent & Home", TxType.EXPENSE, "🏠", true, 50),
        Category("cat_utilities", "Utility Bills", TxType.EXPENSE, "💡", true, 60),
        Category("cat_mobile", "Mobile & Internet", TxType.EXPENSE, "📱", true, 70),
        Category("cat_health", "Health", TxType.EXPENSE, "💊", true, 80),
        Category("cat_education", "Education", TxType.EXPENSE, "📚", true, 90),
        Category("cat_clothing", "Clothing", TxType.EXPENSE, "👕", true, 100),
        Category("cat_entertainment", "Entertainment", TxType.EXPENSE, "🎬", true, 110),
        Category("cat_personal", "Personal Care", TxType.EXPENSE, "🧴", true, 120),
        Category("cat_shopping", "Shopping", TxType.EXPENSE, "🛍️", true, 130),
        Category("cat_travel", "Travel", TxType.EXPENSE, "✈️", true, 140),
        Category("cat_gifts", "Gifts & Donations", TxType.EXPENSE, "🎁", true, 150),
        Category("cat_family", "Family", TxType.EXPENSE, "👨‍👩‍👧", true, 160),
        Category("cat_other_expense", "Other", TxType.EXPENSE, "❓", true, 170),
        // Income
        Category("cat_salary", "Salary", TxType.INCOME, "💼", true, 10),
        Category("cat_business", "Business", TxType.INCOME, "🏪", true, 20),
        Category("cat_freelance", "Freelance", TxType.INCOME, "💻", true, 30),
        Category("cat_investment", "Investment", TxType.INCOME, "📈", true, 40),
        Category("cat_gift_received", "Gift Received", TxType.INCOME, "🎁", true, 50),
        Category("cat_refund", "Refund", TxType.INCOME, "↩️", true, 60),
        Category("cat_bonus", "Bonus", TxType.INCOME, "🏆", true, 70),
        Category("cat_other_income", "Other Income", TxType.INCOME, "💰", true, 80),
    )
}
