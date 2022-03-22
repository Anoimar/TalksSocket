package com.thernat

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.Collections

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(WebSockets)
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/companion") {
            println("New user connected!")
            val currentConnection = Connection(this).also {
                connections += it
            }
            try {
                send("You are connected! " +
                        "You are our ${connections.count()} guest." +
                "You will be known as ${currentConnection.userName}"
                )
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    frame.readText().let { msg ->
                        val msgModel = MsgModel(
                            currentConnection.userName,
                            msg,
                            Instant.now().epochSecond.toString()
                        )
                        connections.forEach {
                            it.session.send(Gson().toJson(msgModel))
                        }
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("${currentConnection.userName} has no access to chat anymore")
                connections -= currentConnection
            }
        }
    }
}