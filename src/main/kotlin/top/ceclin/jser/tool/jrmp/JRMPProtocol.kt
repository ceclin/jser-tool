package top.ceclin.jser.tool.jrmp

import org.joor.Reflect
import sun.rmi.server.MarshalOutputStream
import sun.rmi.transport.TransportConstants
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.lang.reflect.Method
import java.rmi.server.ObjID
import java.security.DigestOutputStream
import java.security.MessageDigest
import kotlin.math.min

/**
 * Some useful method about JRMP protocol.
 *
 * Some useful information can be found in following methods:
 * - [java.rmi.registry.LocateRegistry]
 * - [sun.rmi.server.Util.createProxy]
 * - [java.rmi.server.RemoteObjectInvocationHandler.invoke]
 * - [java.rmi.server.RemoteObjectInvocationHandler.invokeRemoteMethod]
 * - [sun.rmi.server.UnicastRef.invoke]
 */
object JRMPProtocol {

    object SingleOp {
        operator fun invoke(body: ByteArray): ByteArray =
            ByteArrayOutputStream(7 + body.size).also {
                with(DataOutputStream(it)) {
                    writeInt(TransportConstants.Magic)
                    writeShort(TransportConstants.Version.toInt())
                    writeByte((TransportConstants.SingleOpProtocol.toInt()))
                    write(body)
                }
            }.toByteArray()
    }

    object Call {
        /**
         * Generate a JRMP Call packet.
         *
         * Pay attention to [writeParams]:
         * - If param type is primitive, do not use [MarshalOutputStream.writeObject].
         * - If param type is not primitive, you should use [MarshalOutputStream.writeObject].
         */
        operator fun invoke(id: ObjID, op: Int, hash: Long, writeParams: MarshalOutputStream.() -> Unit): ByteArray {
            val out = ByteArrayOutputStream().apply { write(TransportConstants.Call.toInt()) }
            val marshal = MarshalOutputStream(out)
            with(marshal) {
                id.write(this)
                writeInt(op)
                writeLong(hash)
                writeParams()
            }
            return out.toByteArray()
        }
    }

    /**
     * Delegated to [sun.rmi.server.Util.getMethodNameAndDescriptor].
     *
     * You can use javap as an alternative.
     */
    fun computeMethodSignature(method: Method): String =
        Reflect.onClass(sun.rmi.server.Util::class.java)
            .call("getMethodNameAndDescriptor", method)
            .get()

    /**
     * Delegated to [sun.rmi.server.Util.computeMethodHash].
     */
    fun computeMethodHash(method: Method): Long = sun.rmi.server.Util.computeMethodHash(method)

    /**
     * Compute method hash.
     *
     * Its implementation refers to following methods:
     * 1. [sun.rmi.server.Util.computeMethodHash]
     */
    fun computeMethodHash(signature: String): Long {
        var hash = 0L
        val md = MessageDigest.getInstance("SHA")
        // In java 11, OutputStream.nullOutputStream() is better.
        DataOutputStream(DigestOutputStream(ByteArrayOutputStream(127), md))
            .writeUTF(signature)
        val digest = md.digest()
        for (i in 0 until min(8, digest.size)) {
            hash += (digest[i].toLong() and 0xFF) shl (i * 8)
        }
        return hash
    }
}