package nu.henne.backend.runelite.userstats

import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.gson.reflect.TypeToken
import nu.henne.backend.ApiUtil
import nu.henne.backend.runelite.userstats.beans.GitHubTag
import nu.henne.backend.runelite.userstats.beans.OsBuddyStats
import nu.henne.backend.runelite.userstats.beans.UserStatsEntry
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.sql2o.Sql2o
import java.io.InputStreamReader
import java.util.*

@Service
class UserStatsService @Autowired constructor(
    @Qualifier("RuneLite SQL2O") val sql2o: Sql2o
) {
    private companion object {
        val LOGGER: Logger? = LoggerFactory.getLogger(UserStatsService::class.java)

        val OLD_SCHOOL_COUNT_REGEX = "There are currently ([0-9]+) people playing!".toRegex()
    }

    private val last30DaysSupplier = Supplier<List<UserStatsEntry>> {
        sql2o.open().use {
            // 2880 entry limit - limit to 30d of data when entries are 15m apart
            val result = it.createQuery("select * from `user-stats` order by timestamp asc")
                .executeAndFetchLazy(UserStatsEntry::class.java)

            result.asSequence().toList()
        }
    }

    @Volatile
    private var last30Days = Suppliers.memoize(last30DaysSupplier)

    private fun unMemoizeLast30Days() {
        last30Days = Suppliers.memoize(last30DaysSupplier)
    }

    fun getLast30Days(): List<UserStatsEntry> {
        return last30Days.get()
    }

    // Run job every 15 minutes
    @Scheduled(cron = "0 */15 * ? * *")
    private fun collectStats() {
        val timestamp = Date().time / 1000
        val oldSchoolCount = getOldSchoolCount()?: -1
        val osBuddyCount = getOsBuddyCount()?: OsBuddyCount(-1, -1)
        val runeLiteCount = getRuneLiteCount()?: -1

        sql2o.open().use {
            it.createQuery("insert into `user-stats` (timestamp,oldschool_online,osbuddy,osbuddy_online,runelite) " +
                    "values (:timestamp,:oldschool_online,:osbuddy,:osbuddy_online,:runelite)")
                .addParameter("timestamp", timestamp)
                .addParameter("oldschool_online", oldSchoolCount)
                .addParameter("osbuddy", osBuddyCount.total)
                .addParameter("osbuddy_online", osBuddyCount.online)
                .addParameter("runelite", runeLiteCount)
                .executeUpdate()

            unMemoizeLast30Days()
        }
    }

    private fun getOldSchoolCount(): Int? {
        try {
            val request = Request.Builder()
                .url("https://oldschool.runescape.com")
                .build()

            ApiUtil.CLIENT.newCall(request).execute().use {
                val body = it.body()?.string() ?: return null

                val matches = OLD_SCHOOL_COUNT_REGEX.find(body) ?: return null

                return matches.groupValues[1].toInt()
            }
        }
        catch (e: Exception) {
            LOGGER?.warn(null, e)
            return null
        }
    }

    private data class OsBuddyCount(val total: Int, val online: Int)
    private fun getOsBuddyCount(): OsBuddyCount? {
        try {
            val request = Request.Builder()
                .url("https://api.rsbuddy.com/stats")
                .build()

            ApiUtil.CLIENT.newCall(request).execute().use {
                val stats = ApiUtil.GSON.fromJson(InputStreamReader(it.body()?.byteStream()), OsBuddyStats::class.java)
                return OsBuddyCount(stats.online, stats.inGame)
            }
        }
        catch (e: Exception) {
            LOGGER?.warn(null, e)
            return null
        }
    }

    private fun getRuneLiteCount(): Int? {
        try {
            val tagsRequest = Request.Builder()
                .url("https://api.github.com/repos/runelite/runelite/tags")
                .build()

            ApiUtil.CLIENT.newCall(tagsRequest).execute().use {
                val listType = object : TypeToken<List<GitHubTag>>() {}.type
                val tags: List<GitHubTag> = ApiUtil.GSON.fromJson(InputStreamReader(it.body()?.byteStream()), listType)

                if (tags.isEmpty()) {
                    LOGGER?.info("There were no tags for the repository")
                    return null
                }

                val latestTagName = tags.first().name
                val latestVersion = latestTagName.substringAfterLast('-')

                val countRequest = Request.Builder()
                    .url("https://api.runelite.net/runelite-$latestVersion/session/count")
                    .build()

                ApiUtil.CLIENT.newCall(countRequest).execute().use {
                    return ApiUtil.GSON.fromJson(InputStreamReader(it.body()?.byteStream()), Int::class.java)
                }
            }
        }
        catch (e: Exception) {
            LOGGER?.warn(null, e)
            return null
        }
    }
}