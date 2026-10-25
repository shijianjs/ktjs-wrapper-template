package demo.e2e

import cn.hutool.http.server.SimpleServer
import java.io.File
import java.nio.file.Path

/**
 * Serves the production bundle from the test JVM, standing in for the static web server the demo
 * page needs.
 */
object DemoServer {
    private val root: File = File(
        System.getProperty("demo.dist")
            ?: Path.of("build", "dist", "js", "productionExecutable").toString(),
    ).canonicalFile

    private val server: SimpleServer by lazy {
        val started = SimpleServer(0).setRoot(root)
        started.start()
        Runtime.getRuntime().addShutdownHook(Thread { started.rawServer.stop(0) })
        started
    }

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}/"
}
