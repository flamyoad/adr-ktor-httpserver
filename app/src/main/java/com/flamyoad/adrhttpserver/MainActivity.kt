package com.flamyoad.adrhttpserver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
    private val io = Dispatchers.IO
    private val ui = Dispatchers.Main

    private val logger = Logger.getLogger("KtorServer")

    private val server by lazy {
        embeddedServer(CIO, port = 8080, watchPaths = emptyList()) {
            routing {
                get("/") {
//                    call.respond(HttpStatusCode.BadRequest)
                    val ipAddress = call.request.origin.remoteHost
                    call.respondText("<h1>Click accept in your phone</h1>", ContentType.Text.Html)
                    withContext(ui) {
                        showConnectionPrompt(
                            ipAddress,
                            // use webSocket in BE, use Javascript in FE to show accept/decline
                            onAccept = {},
                            onDecline = {},
                        )
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CoroutineScope(io).launch {
            logger.info("Starting server...")
            server.start(wait = true)
        }
        findViewById<TextView>(R.id.txt_ipAddr).apply {
            text = getIpAddressInLocalNetwork() + ":8080"
        }
    }

    override fun onDestroy() {
        server.stop()
        server.stop(gracePeriodMillis = 2500, timeoutMillis = 5000)
        super.onDestroy()
    }

    private fun getIpAddressInLocalNetwork(): String? {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().iterator().asSequence()
        val localAddresses = networkInterfaces.flatMap {
            it.inetAddresses.asSequence()
                .filter { inetAddress ->
                    inetAddress.isSiteLocalAddress && !inetAddress.hostAddress.contains(":") &&
                            inetAddress.hostAddress != "127.0.0.1"
                }
                .map { inetAddress -> inetAddress.hostAddress }
        }
        return localAddresses.firstOrNull()
    }

    private fun showConnectionPrompt(
        ipAddress: String,
        onAccept: () -> Unit = {},
        onDecline: () -> Unit = {}
    ) {
        MaterialDialog(this).show {
            title(text = "Accept connection")
            message(text = "From $ipAddress")
            positiveButton(text = "Accept") {
                onAccept.invoke()
            }
            negativeButton(text = "Decline") {
                onDecline.invoke()
            }
        }.show()
    }
}