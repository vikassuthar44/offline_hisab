package best.app.offlinehisab.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

fun sharePdfUri(context: Context, pdfUri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
}

@RequiresApi(Build.VERSION_CODES.O)
fun filterDateRange(
    filter: FilterOptionType,
    zone: ZoneId = ZoneId.systemDefault()
): Pair<LocalDate?, LocalDate?> {
    val today = LocalDate.now(zone)

    return when (filter) {
        FilterOptionType.Today -> today to today

        FilterOptionType.ThisWeek -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start to today
        }

        FilterOptionType.LastWeek -> {
            val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val startOfLastWeek = startOfThisWeek.minusWeeks(1)
            val endOfLastWeek = startOfLastWeek.plusDays(6)
            startOfLastWeek to endOfLastWeek
        }

        FilterOptionType.ThisMonth -> {
            val start = today.withDayOfMonth(1)
            start to today
        }

        FilterOptionType.LastMonth -> {
            val startOfLastMonth = today.withDayOfMonth(1).minusMonths(1)
            val endOfLastMonth = startOfLastMonth.withDayOfMonth(startOfLastMonth.lengthOfMonth())
            startOfLastMonth to endOfLastMonth
        }

        FilterOptionType.All -> null to null
    }
}



@RequiresApi(Build.VERSION_CODES.O)
fun generateReportRangeTitle(
    filterOptionType: FilterOptionType,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val today = LocalDate.now(zone)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    return when (filterOptionType) {
        FilterOptionType.Today -> {
            "(${today.format(formatter)})"
        }

        FilterOptionType.ThisWeek -> {
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "(${startOfWeek.format(formatter)} to ${today.format(formatter)})"
        }

        FilterOptionType.LastWeek -> {
            val startOfLastWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
            val endOfLastWeek = startOfLastWeek.plusDays(6)
            "(${startOfLastWeek.format(formatter)} to ${endOfLastWeek.format(formatter)})"
        }

        FilterOptionType.ThisMonth -> {
            val startOfMonth = today.withDayOfMonth(1)
            "(${startOfMonth.format(formatter)} to ${today.format(formatter)})"
        }

        FilterOptionType.LastMonth -> {
            val startOfLastMonth = today.withDayOfMonth(1).minusMonths(1)
            val endOfLastMonth = startOfLastMonth.withDayOfMonth(startOfLastMonth.lengthOfMonth())
            "(${startOfLastMonth.format(formatter)} to ${endOfLastMonth.format(formatter)})"
        }

        FilterOptionType.All -> {
            "All Transactions"
        }
    }
}
