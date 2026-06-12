package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getCategoryIcon(iconName: String): ImageVector {
        return when (iconName.lowercase(java.util.Locale.ROOT)) {
            "food" -> Icons.Default.Restaurant
            "shopping" -> Icons.Default.ShoppingCart
            "travel" -> Icons.Default.Flight
            "travel_car" -> Icons.Default.DirectionsCar
            "entertainment" -> Icons.Default.Gamepad
            "medical" -> Icons.Default.LocalHospital
            "personal_care" -> Icons.Default.Face
            "education" -> Icons.Default.School
            "bills" -> Icons.Default.Receipt
            "investments" -> Icons.Default.TrendingUp
            "rent" -> Icons.Default.Home
            "taxes" -> Icons.Default.Percent
            "insurance" -> Icons.Default.Shield
            "gifts" -> Icons.Default.CardGiftcard
            "salary" -> Icons.Default.AttachMoney
            "sold_items" -> Icons.Default.Store
            "coupons" -> Icons.Default.Label
            "bank" -> Icons.Default.AccountBalance
            "cash" -> Icons.Default.AttachMoney
            "card" -> Icons.Default.CreditCard
            "groceries" -> Icons.Default.LocalGroceryStore
            "coffee" -> Icons.Default.LocalCafe
            "fuel" -> Icons.Default.LocalGasStation
            "hotel" -> Icons.Default.Hotel
            "transit" -> Icons.Default.DirectionsBus
            "clothing" -> Icons.Default.Checkroom
            "electronics" -> Icons.Default.Devices
            "family" -> Icons.Default.FamilyRestroom
            "pets" -> Icons.Default.Pets
            "kids" -> Icons.Default.ChildCare
            "movies" -> Icons.Default.Movie
            "music" -> Icons.Default.MusicNote
            "sports" -> Icons.Default.SportsBasketball
            "business" -> Icons.Default.BusinessCenter
            "office" -> Icons.Default.Work
            "savings" -> Icons.Default.Savings
            "phone" -> Icons.Default.Phone
            "internet" -> Icons.Default.Wifi
            "electricity" -> Icons.Default.Bolt
            "water" -> Icons.Default.WaterDrop
            "fitness" -> Icons.Default.FitnessCenter
            "charity" -> Icons.Default.VolunteerActivism
            else -> Icons.Default.Category
        }
    }

    /** Grouped icon palette for the Edit Category picker (Task 5.2 / spec D6). */
    val iconGroups: Map<String, List<String>> = linkedMapOf(
        "Food" to listOf("food", "groceries", "coffee"),
        "Travel" to listOf("travel", "travel_car", "fuel", "hotel", "transit"),
        "Shopping" to listOf("shopping", "clothing", "electronics", "gifts"),
        "Family" to listOf("family", "pets", "kids", "personal_care"),
        "Entertainment" to listOf("entertainment", "movies", "music", "sports"),
        "Business" to listOf("business", "office", "salary", "sold_items"),
        "Finance" to listOf("investments", "savings", "bank", "card", "taxes", "insurance"),
        "Medical" to listOf("medical", "fitness", "charity"),
        "Utilities" to listOf("bills", "phone", "internet", "electricity", "water", "rent"),
        "Miscellaneous" to listOf("education", "coupons", "others")
    )

    val availableIcons = listOf(
        "food" to "Food & Restaurant",
        "shopping" to "Shopping & Cart",
        "travel" to "Travel & Flight",
        "travel_car" to "Car & Commute",
        "entertainment" to "Entertainment & Game",
        "medical" to "Medical & Health",
        "personal_care" to "Personal Care & Spa",
        "education" to "Education & School",
        "bills" to "Bills & Invoice",
        "investments" to "Investments & Stocks",
        "rent" to "Rent & Home",
        "taxes" to "Taxes & Percents",
        "insurance" to "Insurance & Defense",
        "gifts" to "Gifts & Donations",
        "salary" to "Salary & Paycheck",
        "sold_items" to "Sold items",
        "coupons" to "Coupons & Labels",
        "others" to "Generic Category"
    )
}
