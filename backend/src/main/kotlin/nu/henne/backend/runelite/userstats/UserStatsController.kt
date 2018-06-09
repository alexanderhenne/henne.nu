package nu.henne.backend.runelite.userstats

import nu.henne.backend.runelite.userstats.beans.UserStatsEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/runelite/user-stats")
class UserStatsController @Autowired constructor(
    val service: UserStatsService
) {
    @RequestMapping
    fun userStats(): List<UserStatsEntry> {
        return service.getLast30Days()
    }
}