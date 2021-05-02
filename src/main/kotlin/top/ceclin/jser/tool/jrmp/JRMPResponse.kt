package top.ceclin.jser.tool.jrmp

import org.joor.Reflect
import sun.rmi.server.LoaderHandler
import sun.rmi.server.UnicastRef
import sun.rmi.transport.LiveRef
import sun.rmi.transport.TransportConstants
import sun.rmi.transport.tcp.TCPEndpoint
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Proxy
import java.rmi.server.RemoteObject
import java.rmi.server.UID

object JRMPResponse {

    private class MarshalInputStream(input: InputStream?) : sun.rmi.server.MarshalInputStream(input) {
        override fun resolveProxyClass(interfaces: Array<String>): Class<*> = kotlin.runCatching {
            super.resolveProxyClass(interfaces)
        }.getOrElse {
            LoaderHandler.loadProxyClass(
                null,
                arrayOf("java.rmi.Remote"),
                Thread.currentThread().contextClassLoader,
            )
        }
    }

    object Lookup {
        /**
         * Extract result object from lookup response.
         */
        operator fun invoke(packet: ByteArray): Any {
            val input = DataInputStream(ByteArrayInputStream(packet))
            if (input.readByte() != TransportConstants.Return)
                throw IOException("not a return packet")
            val marshal = MarshalInputStream(input)
            marshal.readByte()
            UID.read(marshal)
            return marshal.readObject()
        }

        /**
         * Extract [LiveRef] from [obj] if [obj] is a proxy with [java.rmi.server.RemoteObjectInvocationHandler].
         */
        fun extractLiveRef(obj: Any): LiveRef {
            val remote = Proxy.getInvocationHandler(obj) as RemoteObject
            val ref = remote.ref as UnicastRef
            return ref.liveRef
        }
    }
}