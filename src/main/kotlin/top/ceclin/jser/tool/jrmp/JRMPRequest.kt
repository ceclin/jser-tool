package top.ceclin.jser.tool.jrmp

import java.rmi.server.ObjID

object JRMPRequest {

    private const val INTERFACE_HASH = 4905912898345647071L

    object Lookup {
        /**
         * Generate a stub lookup packet.
         *
         * Its implementation refers to following methods:
         * 1. [sun.rmi.registry.RegistryImpl_Stub.lookup]
         */
        operator fun invoke(name: String): ByteArray =
            JRMPProtocol.SingleOp(
                JRMPProtocol.Call(ObjID(ObjID.REGISTRY_ID), 2, INTERFACE_HASH) { writeObject(name) }
            )
    }
}