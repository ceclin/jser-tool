package top.ceclin.jser.tool.jrmp

import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import org.joor.Reflect
import sun.rmi.transport.LiveRef
import sun.rmi.transport.tcp.TCPEndpoint
import java.io.IOException

/**
 * return [LiveRef.ep] as [TCPEndpoint].
 */
val LiveRef.tcpEndpoint: TCPEndpoint
    get() = Reflect.on(this).get<Any>("ep") as TCPEndpoint // to enable type check


internal fun BytePacketBuilder.compatWriteUTF(str: String) {
    val packet = buildPacket { writeText(str) }
    val size = packet.remaining
    if (size > 65535)
        throw IOException("str too long: $size bytes")
    writeShort(size.toShort())
    writePacket(packet)
}

internal suspend fun ByteReadChannel.compatReadUTF(): String {
    val size = readShort().toInt() and 0xffff
    return readPacket(size).readText()
}