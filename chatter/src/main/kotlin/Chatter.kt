import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val client = HttpClient {
        install(WebSockets)
    }
    runBlocking {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/companion") {
            val outputJob = launch {
                outputMessages()
            }
            val inputJob = launch {
                inputMessages()
            }
            inputJob.join()
            outputJob.cancelAndJoin()
        }
    }
    client.close()
    println("We left chat")
}

suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        for (msg in incoming) {
            val newMsg = msg as? Frame.Text ?: continue
            println(newMsg.readText())
        }
    } catch (e: Exception) {
        println("Exception on output message " + e.localizedMessage)
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages() {
    while (true) {
        (readLine() ?: "").let { msg ->
            if (msg.toLowerCase() == "quit") return
            try {
                send(msg)
            } catch (e: Exception) {
                println("Exception during input message: " + e.localizedMessage)
                return
            }
        }
    }
}