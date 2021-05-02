package top.ceclin.jser.tool.jrmp

import org.junit.Assert.assertEquals
import org.junit.Test
import sun.rmi.registry.RegistryImpl

class JRMPProtocolTest {

    @Test
    fun computeMethodHash() {
        val method = String::class.java.getDeclaredMethod("substring", Int::class.java, Int::class.java)
        assertEquals(
            sun.rmi.server.Util.computeMethodHash(method),
            JRMPProtocol.computeMethodHash("substring(II)Ljava/lang/String;"),
        )
    }

}