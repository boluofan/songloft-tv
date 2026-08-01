package com.songloft.tv.data.repository

import com.songloft.tv.data.api.ApiClient
import com.songloft.tv.data.api.StatsHistoryPage
import com.songloft.tv.data.api.StatsHourlyPoint
import com.songloft.tv.data.api.StatsSummary
import com.songloft.tv.data.api.StatsTrendPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

enum class StatsRange(val label: String) {
    ALL("全部"),
    TODAY("今日"),
    WEEK("本周"),
    MONTH("本月");

    /** 返回 [from, to) 毫秒时间戳区间；ALL 返回空区间 */
    fun timeRange(): Pair<Long?, Long?> {
        if (this == ALL) return null to null
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 86_400_000L
        return when (this) {
            ALL -> null to null
            TODAY -> todayStart to todayEnd
            WEEK -> {
                // 周一为一周起点，周日回退 6 天
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val mondayOffset = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
                cal.add(Calendar.DAY_OF_MONTH, mondayOffset)
                cal.timeInMillis to todayEnd
            }
            MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis to todayEnd
            }
        }
    }
}

@Singleton
class StatsRepository @Inject constructor() {

    private val api get() = ApiClient.getApi()

    suspend fun getSummary(range: StatsRange): Result<StatsSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val (from, to) = range.timeRange()
            val resp = api.getStatsSummary(from, to)
            if (!resp.success) throw Exception(resp.error ?: "统计接口返回失败")
            resp.data ?: throw Exception("统计接口无数据")
        }
    }

    suspend fun getTrends(days: Int = 7): Result<List<StatsTrendPoint>> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api.getStatsTrends(days)
            if (!resp.success) throw Exception("统计接口返回失败")
            resp.data ?: emptyList()
        }
    }

    suspend fun getHourly(): Result<List<StatsHourlyPoint>> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api.getStatsHourly()
            if (!resp.success) throw Exception("统计接口返回失败")
            resp.data ?: emptyList()
        }
    }

    suspend fun getHistory(limit: Int = 20, offset: Int = 0): Result<StatsHistoryPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.getStatsHistory(limit, offset)
                if (!resp.success) throw Exception("统计接口返回失败")
                resp.data ?: StatsHistoryPage()
            }
        }
}
