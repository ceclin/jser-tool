package top.ceclin.jser.tool.jrmp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.util.concurrent.Executors

internal class SocketServer(
    private val port: Int,
    private val handleSocket: suspend (Socket) -> Unit,
) {
    private enum class State {
        READY, RUNNING, STOPPED
    }

    private val state = atomic(State.READY)

    private val executorService = Executors.newFixedThreadPool(1)

    private val scope = CoroutineScope(executorService.asCoroutineDispatcher())

    private lateinit var server: ServerSocket

    fun start() {
        if (state.compareAndSet(expect = State.READY, update = State.RUNNING)) {
            server = aSocket(ActorSelectorManager(Dispatchers.IO))
                .tcp().tcpNoDelay()
                .bind(InetSocketAddress(port))
            scope.launch {
                supervisorScope {
                    while (true) {
                        val socket = server.accept()
                        launch(Dispatchers.Default) {
                            try {
                                handleSocket(socket)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                socket.dispose()
                            }
                        }
                    }
                }
            }
        } else {
            throw IllegalStateException("current state: ${state.value}")
        }
    }

    fun stop() {
        if (state.compareAndSet(expect = State.RUNNING, update = State.STOPPED)) {
            scope.cancel()
            server.close()
            executorService.shutdown()
        } else {
            throw IllegalStateException("current state: ${state.value}")
        }
    }
}