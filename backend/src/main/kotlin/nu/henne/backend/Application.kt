package nu.henne.backend

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.sql2o.Sql2o

@SpringBootApplication
@EnableScheduling
class Application {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }

    @Bean("RuneLite SQL2O")
    fun runeLiteSql2o(): Sql2o {
        val dataSource = DataSourceBuilder.create()
            .driverClassName("org.sqlite.JDBC")
            .url("jdbc:sqlite:data/runelite.db")
            .build()

        return Sql2o(dataSource)
    }
}