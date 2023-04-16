import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import space.jetbrains.api.runtime.stacktrace.BOUNDARY_STACK_TRACE_ELEMENT_CLASS_NAME
import space.jetbrains.api.runtime.stacktrace.withPreservedStacktrace
import kotlin.test.junit5.JUnit5Asserter.fail

class RequestStacktraceTest {
    @Test
    fun testRequestStacktrace() {
        runBlocking {
            try {
                withPreservedStacktrace("test message") {
                    suspendDuringHttpRequestThenThrow()
                }
            } catch (e: HttpRequestTimeoutException) {
                assert(e.stackTrace.any { it.className == BOUNDARY_STACK_TRACE_ELEMENT_CLASS_NAME })
                return@runBlocking
            }
            fail("TestException should be thrown")
        }
    }
}

private suspend fun suspendDuringHttpRequestThenThrow() {
    embeddedServer(Netty, port = 9003, host = "0.0.0.0", module = Application::appConfig)
        .start(wait = false)

    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10
        }
    }.use { client ->
        // suspension point here
        client.get("http://0.0.0.0:9003")
    }
}

fun Application.appConfig() {
    install(Routing) {
        get("/") {
            delay(1000)
            call.respondText("test")
        }
    }
}
