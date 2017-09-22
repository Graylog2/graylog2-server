/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security.ldap;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.graylog2.plugin.DocsHelper;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class LdapConnector {
    private static final Logger LOG = LoggerFactory.getLogger(LdapConnector.class);

    private static final String ATTRIBUTE_UNIQUE_MEMBER = "uniqueMember";
    private static final String ATTRIBUTE_MEMBER = "member";
    private static final String ATTRIBUTE_MEMBER_UID = "memberUid";

    private final int connectionTimeout;

    @Inject
    public LdapConnector(@Named("ldap_connection_timeout") int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public LdapNetworkConnection connect(LdapConnectionConfig config) throws LdapException {
        final LdapNetworkConnection connection = new LdapNetworkConnection(config);
        connection.setTimeOut(connectionTimeout);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Connecting to LDAP server {}:{}, binding with user {}",
                      config.getLdapHost(), config.getLdapPort(), config.getName());
        }

        // this will perform an anonymous bind if there were no system credentials
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("ldap-connector-%d").build();
        final SimpleTimeLimiter timeLimiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor(threadFactory));
        @SuppressWarnings("unchecked")
        final Callable<Boolean> timeLimitedConnection = timeLimiter.newProxy(
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return connection.connect();
                    }
                }, Callable.class,
                connectionTimeout, TimeUnit.MILLISECONDS);
        try {
            final Boolean connected = timeLimitedConnection.call();
            if (!connected) {
                return null;
            }
        } catch (UncheckedTimeoutException e) {
            LOG.error("Timed out connecting to LDAP server", e);
            throw new LdapException("Could not connect to LDAP server", e.getCause());
        } catch (LdapException e) {
            throw e;
        } catch (Exception e) {
            // unhandled different exception, should really not happen here.
            throw new LdapException("Unexpected error connecting to LDAP", e);
        }
        connection.bind();

        return connection;
    }

    @Nullable
    public LdapEntry search(LdapNetworkConnection connection,
                            String searchBase,
                            String searchPattern,
                            String displayNameAttribute,
                            String principal,
                            boolean activeDirectory,
                            String groupSearchBase,
                            String groupIdAttribute,
                            String groupSearchPattern) throws LdapException, CursorException {
        final LdapEntry ldapEntry = new LdapEntry();
        final Set<String> groupDns = Sets.newHashSet();

        final String filter = new MessageFormat(searchPattern, Locale.ENGLISH).format(new Object[]{sanitizePrincipal(principal)});
        if (LOG.isTraceEnabled()) {
            LOG.trace("Search {} for {}, starting at {}",
                      activeDirectory ? "ActiveDirectory" : "LDAP", filter, searchBase);
        }

        try (final EntryCursor entryCursor = connection.search(searchBase,
                filter,
                SearchScope.SUBTREE,
                groupIdAttribute, displayNameAttribute, "dn", "uid", "userPrincipalName", "mail", "rfc822Mailbox", "memberOf", "isMemberOf")
        ) {
            final Iterator<Entry> it = entryCursor.iterator();
            if (it.hasNext()) {
                final Entry e = it.next();
                // always set the proper DN for the entry, we need it for group matching
                ldapEntry.setDn(e.getDn().getName());

                // for generic LDAP use the dn of the entry for the subsequent bind, active directory needs the userPrincipalName attribute (set below)
                if (!activeDirectory) {
                    ldapEntry.setBindPrincipal(e.getDn().getName());
                }

                for (Attribute attribute : e.getAttributes()) {
                    if (activeDirectory && "userPrincipalName".equalsIgnoreCase(attribute.getId())) {
                        ldapEntry.setBindPrincipal(attribute.getString());
                    }
                    if (attribute.isHumanReadable()) {
                        ldapEntry.put(attribute.getId(), Joiner.on(", ").join(attribute.iterator()));
                    }
                    // ActiveDirectory (memberOf) and Sun Directory Server (isMemberOf)
                    if ("memberOf".equalsIgnoreCase(attribute.getId()) || "isMemberOf".equalsIgnoreCase(attribute.getId())) {
                        for (Value<?> group : attribute) {
                            groupDns.add(group.getString());
                        }

                    }
                }
            } else {
                LOG.trace("No LDAP entry found for filter {}", filter);
                return null;
            }
            if (!groupDns.isEmpty() && !isNullOrEmpty(groupSearchBase) && !isNullOrEmpty(groupIdAttribute)) {
                // user had a memberOf attribute which contained group references. resolve each group and collect group names
                // according to groupIdAttribute if present
                try {
                    for (String groupDn : groupDns) {
                        LOG.trace("Looking up group {}", groupDn);
                        try {
                            Entry group = connection.lookup(groupDn, groupIdAttribute);
                            // The groupDn lookup can return null if the group belongs to a different domain and the
                            // connection user does not have the permissions to lookup details.
                            // See: https://github.com/Graylog2/graylog2-server/issues/1453
                            if (group != null) {
                                final Attribute groupId = group.get(groupIdAttribute);
                                LOG.trace("Resolved {} to group {}", groupDn, groupId);
                                if (groupId != null) {
                                    final String string = groupId.getString();
                                    ldapEntry.addGroups(Collections.singleton(string));
                                }
                            } else {
                                LOG.debug("Unable to lookup group: {}", groupDn);
                            }
                        } catch (LdapException e) {
                            LOG.warn("Error while looking up group " + groupDn, e);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Unexpected error during LDAP group resolution", e);
                }
            }
            if (ldapEntry.getGroups().isEmpty() && !isNullOrEmpty(groupSearchBase) && !isNullOrEmpty(groupIdAttribute) && !isNullOrEmpty(groupSearchPattern)) {
                ldapEntry.addGroups(findGroups(connection,
                                               groupSearchBase,
                                               groupSearchPattern,
                                               groupIdAttribute,
                                               ldapEntry
                ));
                LOG.trace("LDAP search found entry for DN {} with search filter {}: {}",
                          ldapEntry.getDn(),
                          filter,
                          ldapEntry);
            } else {
                if (groupDns.isEmpty()) {
                    LOG.info("LDAP group search base, id attribute or object class missing, not iterating over LDAP groups.");
                }
            }
            return ldapEntry;
        } catch (IOException e) {
            LOG.debug("Error while closing cursor", e);
            return null;
        }
    }

    public Set<String> findGroups(LdapNetworkConnection connection,
                                  String groupSearchBase,
                                  String groupSearchPattern,
                                  String groupIdAttribute,
                                  @Nullable LdapEntry ldapEntry) {
        final Set<String> groups = Sets.newHashSet();

        try (final EntryCursor groupSearch = connection.search(
                groupSearchBase,
                groupSearchPattern,
                SearchScope.SUBTREE,
                "objectClass", ATTRIBUTE_UNIQUE_MEMBER, ATTRIBUTE_MEMBER, ATTRIBUTE_MEMBER_UID, groupIdAttribute)) {
            LOG.trace("LDAP search for groups: {} starting at {}", groupSearchPattern, groupSearchBase);
            for (Entry e : groupSearch) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Group Entry: {}", e.toString("  "));
                }
                if (! e.containsAttribute(groupIdAttribute)) {
                    LOG.warn("Unknown group id attribute {}, skipping group entry {}", groupIdAttribute, e);
                    continue;
                }
                final String groupId = e.get(groupIdAttribute).getString();
                if (ldapEntry == null) {
                    // no membership lookup possible (we have no user), simply collect the found group names
                    groups.add(groupId);
                } else {
                    // test if the given dn parameter is actually member of any of the found groups
                    String memberAttribute;
                    if (e.hasObjectClass("groupOfUniqueNames")) {
                        memberAttribute = ATTRIBUTE_UNIQUE_MEMBER;
                    } else if (e.hasObjectClass("groupOfNames") || e.hasObjectClass("group")) {
                        memberAttribute = ATTRIBUTE_MEMBER;
                    } else if (e.hasObjectClass("posixGroup")) {
                        memberAttribute = ATTRIBUTE_MEMBER_UID;
                    } else {
                        // Trying auto detection of the member attribute. This should be configurable!
                        if (e.containsAttribute(ATTRIBUTE_UNIQUE_MEMBER)) {
                            memberAttribute = ATTRIBUTE_UNIQUE_MEMBER;
                        } else if (e.containsAttribute(ATTRIBUTE_MEMBER_UID)) {
                            memberAttribute = ATTRIBUTE_MEMBER_UID;
                        } else {
                            memberAttribute = ATTRIBUTE_MEMBER;
                        }
                        LOG.warn(
                                "Unable to auto-detect the LDAP group object class, assuming '{}' is the correct attribute.",
                                memberAttribute);
                    }
                    final Attribute members = e.get(memberAttribute);
                    if (members != null) {
                        final String dn = normalizedDn(ldapEntry.getDn());
                        final String uid = ldapEntry.get("uid");

                        for (Value<?> member : members) {
                            LOG.trace("DN {} == {} member?", dn, member.getString());
                            if (dn != null && dn.equalsIgnoreCase(normalizedDn(member.getString()))) {
                                groups.add(groupId);
                            } else {
                                // The posixGroup object class is using the memberUid attribute for group members.
                                // Since the memberUid attribute takes uid values instead of dn values, we have to
                                // check against the uid attribute of the user.
                                if (!isNullOrEmpty(uid) && uid.equalsIgnoreCase(member.getString())) {
                                    LOG.trace("UID {} == {} member?", uid, member.getString());
                                    groups.add(groupId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn(
                    "Unable to iterate over user's groups, unable to perform group mapping. Graylog does not support " +
                            "LDAP referrals at the moment. Please see " +
                            DocsHelper.PAGE_LDAP_TROUBLESHOOTING.toString() + " for more information.",
                    ExceptionUtils.getRootCause(e));
        }

        return groups;
    }

    /**
     * When the given string is a DN, the method ensures that the DN gets normalized so it can be used in string
     * comparison.
     *
     * If the string is not a DN, the method just returns it.
     *
     * Examples:
     *
     * String is a DN:
     *   input  = "cn=John Doe, ou=groups, ou=system"
     *   output = "cn=John Doe,ou=groups,ou=system"
     *
     * String is not a DN:
     *   input  = "john"
     *   output = "john"
     *
     * This behavior is needed because for some values we don't know if the value is a DN or not. (i.e. group member values)
     *
     * See: https://github.com/Graylog2/graylog2-server/issues/1790
     *
     * @param dn denormalized DN string
     * @return normalized DN string
     */
    @Nullable
    private String normalizedDn(String dn) {
        if (isNullOrEmpty(dn)) {
            return dn;
        } else {
            try {
                return new Dn(dn).getNormName();
            } catch (LdapInvalidDnException e) {
                LOG.debug("Invalid DN", e);
                return dn;
            }
        }
    }

    public Set<String> listGroups(LdapNetworkConnection connection,
                                  String groupSearchBase,
                                  String groupSearchPattern,
                                  String groupIdAttribute) {
        return findGroups(connection, groupSearchBase, groupSearchPattern, groupIdAttribute, null);
    }

    /**
     * Escapes any special chars (RFC 4515) from a string representing a
     * a search filter assertion value.
     *
     * @param input The input string.
     * @return A assertion value string ready for insertion into a
     * search filter string.
     */
    private String sanitizePrincipal(final String input) {
        String s = "";

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '*') {
                // escape asterisk
                s += "\\2a";
            } else if (c == '(') {
                // escape left parenthesis
                s += "\\28";
            } else if (c == ')') {
                // escape right parenthesis
                s += "\\29";
            } else if (c == '\\') {
                // escape backslash
                s += "\\5c";
            } else if (c == '\u0000') {
                // escape NULL char
                s += "\\00";
            } else if (c <= 0x7f) {
                // regular 1-byte UTF-8 char
                s += String.valueOf(c);
            } else if (c >= 0x080) {
                // higher-order 2, 3 and 4-byte UTF-8 chars
                byte[] utf8bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);

                for (byte b : utf8bytes) {
                    s += String.format(Locale.ENGLISH, "\\%02x", b);
                }
            }
        }

        return s;
    }

    public boolean authenticate(LdapNetworkConnection connection, String principal, String credentials) throws LdapException {
        checkArgument(!isNullOrEmpty(principal), "Binding with empty principal is forbidden.");
        checkArgument(!isNullOrEmpty(credentials), "Binding with empty credentials is forbidden.");

        final BindRequestImpl bindRequest = new BindRequestImpl();
        bindRequest.setName(principal);
        bindRequest.setCredentials(credentials);
        LOG.trace("Re-binding with DN {} using password", principal);
        final BindResponse bind = connection.bind(bindRequest);
        if (!bind.getLdapResult().getResultCode().equals(ResultCodeEnum.SUCCESS)) {
            LOG.trace("Re-binding DN {} failed", principal);
            throw new RuntimeException(bind.toString());
        }
        LOG.trace("Binding DN {} did not throw, connection authenticated: {}", principal, connection.isAuthenticated());
        return connection.isAuthenticated();
    }
}
