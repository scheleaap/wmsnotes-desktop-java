package info.maaskant.wmsnotes.desktop.app

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Configuration
class OtherConfiguration {

    @Qualifier
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    annotation class AppDirectory

    @Bean
    @Singleton
    @AppDirectory
    fun appDirectory(@Value("\${rootDirectory:#{null}}") rootDirectory: String?): File {
        return if (rootDirectory != null) {
            File(rootDirectory)
        } else {
            if (System.getProperty("os.name").startsWith("Windows")) {
                File(System.getenv("APPDATA")).resolve("WMS Notes").resolve("Desktop")
            } else {
                File(System.getProperty("user.home")).resolve(".wmsnotes").resolve("desktop")
            }
        }
    }

    @Bean
    @Singleton
    fun kryoPool(): Pool<Kryo> {
        return object : Pool<Kryo>(true, true) {
            override fun create(): Kryo = Kryo()
        }
    }

}