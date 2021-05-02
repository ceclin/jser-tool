package top.ceclin.jser.tool.ldap

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.util.Debug
import javax.net.ssl.SSLSocketFactory

/**
 * https://docs.ldap.com/ldap-sdk/docs/in-memory-directory-server.html
 * https://tools.ietf.org/html/rfc2713
 */
internal object LdapCommon {

    private const val BASE_DN = "dc=example,dc=com"

    init {
        Debug.setEnabled(true)
    }

    /**
     * corresponding search result MUST be success and MUST have no entries
     */
    val voidSearchRequest = SearchRequest(
        BASE_DN,
        SearchScope.BASE,
        Filter.createPresenceFilter("void")
    )

    fun createDirectoryServer(
        port: Int,
        configure: InMemoryDirectoryServerConfig.() -> Unit,
    ): InMemoryDirectoryServer {
        val config = InMemoryDirectoryServerConfig(BASE_DN).apply {
            setListenerConfigs(
                InMemoryListenerConfig.createLDAPConfig(
                    "any", null, port,
                    SSLSocketFactory.getDefault() as SSLSocketFactory
                )
            )
        }.apply(configure)
        return InMemoryDirectoryServer(config).apply {
            // add an entry to make lookup not fail immediately
            addEntries(Entry("dn: $BASE_DN", "objectClass: top", "objectClass: domain", "dc: example"))
        }
    }
}