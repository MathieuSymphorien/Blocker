package com.mathieu.blocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UsageStatsEntity)

    @Query("SELECT * FROM usage_stats WHERE date = :date AND packageName = :packageName")
    suspend fun getForAppAndDate(date: String, packageName: String): UsageStatsEntity?

    @Query("""
        SELECT COALESCE(date, :date) as date,
               '' as packageName,
               COALESCE(SUM(scrollCount), 0) as scrollCount,
               COALESCE(SUM(scrollTimeMs), 0) as scrollTimeMs,
               COALESCE(SUM(videoCount), 0) as videoCount,
               COALESCE(SUM(nonVideoCount), 0) as nonVideoCount,
               COALESCE(SUM(openCount), 0) as openCount,
               COALESCE(SUM(returnCount), 0) as returnCount
        FROM usage_stats
        WHERE date = :date
    """)
    fun getDailyTotals(date: String): Flow<UsageStatsEntity>

    @Query("""
        SELECT date,
               '' as packageName,
               SUM(scrollCount) as scrollCount,
               SUM(scrollTimeMs) as scrollTimeMs,
               SUM(videoCount) as videoCount,
               SUM(nonVideoCount) as nonVideoCount,
               SUM(openCount) as openCount,
               SUM(returnCount) as returnCount
        FROM usage_stats
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyTotalsRange(startDate: String, endDate: String): Flow<List<UsageStatsEntity>>

    @Query("""
        SELECT '' as date,
               '' as packageName,
               AVG(daily_scroll) as scrollCount,
               AVG(daily_time) as scrollTimeMs,
               AVG(daily_video) as videoCount,
               AVG(daily_nonvideo) as nonVideoCount,
               AVG(daily_open) as openCount,
               AVG(daily_return) as returnCount
        FROM (
            SELECT SUM(scrollCount) as daily_scroll,
                   SUM(scrollTimeMs) as daily_time,
                   SUM(videoCount) as daily_video,
                   SUM(nonVideoCount) as daily_nonvideo,
                   SUM(openCount) as daily_open,
                   SUM(returnCount) as daily_return
            FROM usage_stats
            WHERE date >= :startDate AND date <= :endDate
            GROUP BY date
        )
    """)
    fun getAveragesForRange(startDate: String, endDate: String): Flow<UsageStatsEntity?>

    @Query("SELECT * FROM usage_stats WHERE date = :date ORDER BY scrollTimeMs DESC")
    fun getStatsPerAppForDate(date: String): Flow<List<UsageStatsEntity>>

    @Query("SELECT COALESCE(MIN(date), '') FROM usage_stats")
    suspend fun getOldestDate(): String

    @Query("""
        SELECT packageName,
               '' as date,
               SUM(scrollCount) as scrollCount,
               SUM(scrollTimeMs) as scrollTimeMs,
               SUM(videoCount) as videoCount,
               SUM(nonVideoCount) as nonVideoCount,
               SUM(openCount) as openCount,
               SUM(returnCount) as returnCount
        FROM usage_stats
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY packageName
        ORDER BY openCount DESC
    """)
    suspend fun getPerAppTotalsForRange(startDate: String, endDate: String): List<UsageStatsEntity>

    @Query("""
        SELECT '' as date,
               '' as packageName,
               COALESCE(SUM(scrollCount), 0) as scrollCount,
               COALESCE(SUM(scrollTimeMs), 0) as scrollTimeMs,
               COALESCE(SUM(videoCount), 0) as videoCount,
               COALESCE(SUM(nonVideoCount), 0) as nonVideoCount,
               COALESCE(SUM(openCount), 0) as openCount,
               COALESCE(SUM(returnCount), 0) as returnCount
        FROM usage_stats
    """)
    suspend fun getAllTimeTotals(): UsageStatsEntity?

    @Query("""
        SELECT date,
               '' as packageName,
               SUM(scrollCount) as scrollCount,
               SUM(scrollTimeMs) as scrollTimeMs,
               SUM(videoCount) as videoCount,
               SUM(nonVideoCount) as nonVideoCount,
               SUM(openCount) as openCount,
               SUM(returnCount) as returnCount
        FROM usage_stats
        WHERE date >= :startDate
        GROUP BY date
    """)
    suspend fun getDailyTotalsForStreak(startDate: String): List<UsageStatsEntity>

    @Query("""
        SELECT '' as date, '' as packageName,
               COALESCE(SUM(scrollCount), 0) as scrollCount,
               COALESCE(SUM(scrollTimeMs), 0) as scrollTimeMs,
               COALESCE(SUM(videoCount), 0) as videoCount,
               COALESCE(SUM(nonVideoCount), 0) as nonVideoCount,
               COALESCE(SUM(openCount), 0) as openCount,
               COALESCE(SUM(returnCount), 0) as returnCount
        FROM (
            SELECT SUM(scrollCount) as scrollCount, SUM(scrollTimeMs) as scrollTimeMs,
                   SUM(videoCount) as videoCount, SUM(nonVideoCount) as nonVideoCount,
                   SUM(openCount) as openCount, SUM(returnCount) as returnCount
            FROM usage_stats WHERE date >= :startDate AND date <= :endDate GROUP BY date
        )
    """)
    suspend fun getTotalsForRange(startDate: String, endDate: String): UsageStatsEntity?

    @Query("""
        UPDATE usage_stats
        SET scrollCount = scrollCount + :count, scrollTimeMs = scrollTimeMs + :timeMs
        WHERE date = :date AND packageName = :packageName
    """)
    suspend fun incrementScroll(date: String, packageName: String, count: Int, timeMs: Long)

    @Query("""
        UPDATE usage_stats
        SET openCount = openCount + 1
        WHERE date = :date AND packageName = :packageName
    """)
    suspend fun incrementOpenCount(date: String, packageName: String)

    @Query("""
        UPDATE usage_stats
        SET returnCount = returnCount + 1
        WHERE date = :date AND packageName = :packageName
    """)
    suspend fun incrementReturnCount(date: String, packageName: String)

    @Query("""
        UPDATE usage_stats
        SET videoCount = videoCount + 1
        WHERE date = :date AND packageName = :packageName
    """)
    suspend fun incrementVideoCount(date: String, packageName: String)

    @Query("""
        UPDATE usage_stats
        SET nonVideoCount = nonVideoCount + 1
        WHERE date = :date AND packageName = :packageName
    """)
    suspend fun incrementNonVideoCount(date: String, packageName: String)
}
