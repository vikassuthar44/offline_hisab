package best.app.offlinehisab.utils


// FilterOption.kt
enum class FilterOptionType(val displayName: String) {
    Today(displayName = "Today"),
    ThisWeek(displayName = "This Week"),
    LastWeek(displayName = "Last week"),
    ThisMonth(displayName = "This Month"),
    LastMonth(displayName = "Last Month"),
    All(displayName = "All Transaction")

}

class FilterOption {
    companion object {
        fun getAllFilterOption(): List<FilterOptionType> {
            return listOf(
                FilterOptionType.Today,
                FilterOptionType.ThisWeek,
                FilterOptionType.ThisMonth,
                FilterOptionType.All,
            )
        }

        fun getFilterOptionTypeDescription(optionType: FilterOptionType): String {
            return when (optionType) {
                FilterOptionType.Today -> "Only today's transactions"
                FilterOptionType.ThisWeek -> "Current Week (incl. today)"
                FilterOptionType.LastWeek -> "Last Week (incl. today)"
                FilterOptionType.ThisMonth -> "Current Month (incl. today)"
                FilterOptionType.LastMonth -> "Last Month (incl. today)"
                FilterOptionType.All -> "All transactions"
            }
        }
    }
}