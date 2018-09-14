package org.graylog2.lookup.adapters.dnslookup;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsPtrRecord;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.DnsServerAddressStreamProvider;
import io.netty.resolver.dns.SequentialDnsServerAddressStreamProvider;
import io.netty.util.concurrent.Future;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DnsClient {

    private static final Logger LOG = LoggerFactory.getLogger(DnsClient.class);

    private static final int DEFAULT_DNS_PORT = 53;
    private static final Pattern VALID_HOSTNAME_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

    // Use fully qualified reverse lookup domain names (with dot at end).
    private static final String IP_4_REVERSE_SUFFIX = ".in-addr.arpa.";
    private static final String IP_6_REVERSE_SUFFIX = ".ip6.arpa.";

    private static final String IP_4_VERSION = "IPv4";
    private static final String IP_6_VERSION = "IPv6";

    // Used to convert binary IPv6 address to hex.
    private static final char[] HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray();

    private NioEventLoopGroup nettyEventLoop;
    private DnsNameResolver resolver;

    public void start(String dnsServerIps, long requestTimeout) {

        LOG.debug("Attempting to start DNS client");
        List<InetSocketAddress> iNetDnsServerIps = parseServerIpAddresses(dnsServerIps);

        nettyEventLoop = new NioEventLoopGroup();

        DnsNameResolverBuilder dnsNameResolverBuilder = new DnsNameResolverBuilder(nettyEventLoop.next());
        dnsNameResolverBuilder.channelType(NioDatagramChannel.class).queryTimeoutMillis(requestTimeout);

        // Specify custom DNS server if provided. If not, use local network adapter address.
        if (CollectionUtils.isNotEmpty(iNetDnsServerIps)) {

            LOG.debug("Attempting to start DNS client with server IPs [{}] on port [{}] with timeout [{}]",
                      dnsServerIps, DEFAULT_DNS_PORT, requestTimeout);

            DnsServerAddressStreamProvider dnsServer = new SequentialDnsServerAddressStreamProvider(iNetDnsServerIps);
            dnsNameResolverBuilder.nameServerProvider(dnsServer);
        } else {
            LOG.debug("Attempting to start DNS client with the local network adapter DNS server address on port [{}] with timeout [{}]",
                      DEFAULT_DNS_PORT, requestTimeout);
        }

        resolver = dnsNameResolverBuilder.build();

        LOG.debug("DNS client startup successful");
    }

    private List<InetSocketAddress> parseServerIpAddresses(String dnsServerIps) {

        // Parse and prepare DNS server IP addresses for Netty.
        return StreamSupport
                // Split comma-separated sever IP:port combos.
                .stream(Splitter.on(",").trimResults().omitEmptyStrings().split(dnsServerIps).spliterator(), false)
                // Parse as HostAndPort objects (allows convenient handling of port provided after colon).
                .map(hostAndPort -> HostAndPort.fromString(hostAndPort).withDefaultPort(DnsClient.DEFAULT_DNS_PORT))
                // Convert HostAndPort > InetSocketAddress as required by Netty.
                .map(hostAndPort -> new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()))
                .collect(Collectors.toList());
    }

    public void stop() {

        LOG.debug("Attempting to stop DNS client");

        if (nettyEventLoop == null) {
            LOG.error("DNS resolution event loop not initialized");
            return;
        }

        // Shutdown event loop (required by Netty).
        Future<?> shutdownFuture = nettyEventLoop.shutdownGracefully();
        shutdownFuture.addListener(future -> LOG.debug("DNS client shutdown successful"));
    }

    public List<ADnsAnswer> resolveIPv4AddressForHostname(String hostName, boolean includeIpVersion)
            throws InterruptedException, ExecutionException, UnknownHostException {

        return resolveIpAddresses(hostName, DnsRecordType.A, includeIpVersion);
    }

    public List<ADnsAnswer> resolveIPv6AddressForHostname(String hostName, boolean includeIpVersion)
            throws InterruptedException, ExecutionException, UnknownHostException {

        return resolveIpAddresses(hostName, DnsRecordType.AAAA, includeIpVersion);
    }

    private List<ADnsAnswer> resolveIpAddresses(String hostName, DnsRecordType dnsRecordType, boolean includeIpVersion)
            throws InterruptedException, ExecutionException {

        LOG.debug("Attempting to resolve [{}] records for [{}]", dnsRecordType, hostName);

        if (isShutdown()) {
            throw new DnsClientNotRunningException();
        }

        validateHostName(hostName);

        DefaultDnsQuestion aRecordDnsQuestion = new DefaultDnsQuestion(hostName, dnsRecordType);

        /* The DnsNameResolver.resolveAll(DnsQuestion) method handles all redirects through CNAME records to
         * ultimately resolve a list of IP addresses with TTL values. */
        return resolver.resolveAll(aRecordDnsQuestion).sync().get().stream()
                       .map(dnsRecord -> decodeDnsRecord(dnsRecord, includeIpVersion))
                       .filter(Objects::nonNull) // Removes any entries which the IP address could not be extracted for.
                       .collect(Collectors.toList());
    }

    /**
     * Picks out the IP address and TTL from the answer response for each record.
     */
    private static ADnsAnswer decodeDnsRecord(DnsRecord dnsRecord, boolean includeIpVersion) {

        if (dnsRecord == null) {
            return null;
        }

        LOG.trace("Attempting to decode DNS record [{}]", dnsRecord);

        /* Read data from DNS record response. The data is a binary representation of the IP address
         * IPv4 address: 32 bits, IPv6 address: 128 bits */
        ByteBuf byteBuf;
        byte[] ipAddressBytes;
        DefaultDnsRawRecord dnsRawRecord = (DefaultDnsRawRecord) dnsRecord;
        try {
            byteBuf = dnsRawRecord.content();
            ipAddressBytes = new byte[byteBuf.readableBytes()];
            int readerIndex = byteBuf.readerIndex();
            byteBuf.getBytes(readerIndex, ipAddressBytes);
        } finally {
            /* Must manually release references on dnsRawRecord object since the DefaultDnsRawRecord class
             * extends ReferenceCounted. This also releases the above ByteBuf, since DefaultDnsRawRecord is
             * the holder for it. */
            dnsRawRecord.release();
        }

        LOG.trace("The IP address has [{}] bytes", ipAddressBytes.length);

        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByAddress(ipAddressBytes); // Takes care of correctly creating an IPv4 or IPv6 address.
        } catch (UnknownHostException e) {
            // This should not happen.
            LOG.error("Could not extract IP address from DNS entry [{}]. Cause [{}]", dnsRecord.toString(), ExceptionUtils.getRootCauseMessage(e));
            return null;
        }

        LOG.trace("The resulting IP address is [{}]", ipAddress.getHostAddress());

        ADnsAnswer.Builder builder = ADnsAnswer.builder()
                                               .ipAddress(ipAddress.getHostAddress())
                                               .dnsTTL(dnsRecord.timeToLive());

        if (includeIpVersion) {
            builder.ipVersion(ipAddress instanceof Inet4Address ? IP_4_VERSION : IP_6_VERSION);
        }

        return builder.build();
    }

    public PtrDnsAnswer reverseLookup(String ipAddress) throws InterruptedException, ExecutionException {

        LOG.debug("Attempting to perform reverse lookup for IP address [{}]", ipAddress);

        if (isShutdown()) {
            throw new DnsClientNotRunningException();
        }

        validateIpAddress(ipAddress);

        String inverseAddressFormat = getInverseAddressFormat(ipAddress);

        DnsResponse content = null;
        try {
            content = resolver.query(new DefaultDnsQuestion(inverseAddressFormat, DnsRecordType.PTR)).sync().get().content();
            for (int i = 0; i < content.count(DnsSection.ANSWER); i++) {

                // Return the first PTR record, because there should be only one as per
                // http://tools.ietf.org/html/rfc1035#section-3.5
                DnsRecord dnsRecord = content.recordAt(DnsSection.ANSWER, i);
                if (dnsRecord instanceof DefaultDnsPtrRecord) {

                    DefaultDnsPtrRecord ptrRecord = (DefaultDnsPtrRecord) dnsRecord;
                    PtrDnsAnswer.Builder dnsAnswerBuilder = PtrDnsAnswer.builder();

                    String hostname = ptrRecord.hostname();
                    LOG.trace("PTR record retrieved with hostname [{}]", hostname);

                    try {
                        parseReverseLookupDomain(dnsAnswerBuilder, hostname);
                    } catch (IllegalArgumentException e) {
                        LOG.debug("Reverse lookup of [{}] was partially successful. The DNS server returned [{}], " +
                                  "which is an invalid host name. The \"domain\" field will be left blank.",
                                  ipAddress, hostname);
                        dnsAnswerBuilder.domain("");
                    }

                    return dnsAnswerBuilder.dnsTTL(ptrRecord.timeToLive())
                                           .build();
                }
            }
        } finally {
            if (content != null) {
                // Must manually release references on content object since the DnsResponse class extends ReferenceCounted
                content.release();
            }
        }

        return null;
    }

    /**
     * Extract the domain name (without subdomain). The Guava {@link InternetDomainName} implementation
     * provides a method to correctly handle this (and handles special cases for TLDs with multiple
     * names. eg. for lb01.store.amazon.co.uk, only amazon.co.uk would be extracted).
     * It uses https://publicsuffix.org behind the scenes.
     * <p>
     * Some domains (eg. completely randomly defined PTR domains) are not considered to have a public
     * suffix according to Guava. For those, the only option is to manually extract the domain with
     * string operations. This should be a rare case.
     */
    public static void parseReverseLookupDomain(PtrDnsAnswer.Builder dnsAnswerBuilder, String hostname) {

        dnsAnswerBuilder.fullDomain(hostname);

        InternetDomainName internetDomainName = InternetDomainName.from(hostname);
        if (internetDomainName.hasPublicSuffix()) {

            // Use Guava to extract domain name.
            InternetDomainName topDomainName = internetDomainName.topDomainUnderRegistrySuffix();
            dnsAnswerBuilder.domain(topDomainName.toString());
        } else {

            // Manually extract domain name.
            // Eg. for hostname test.some-domain.com, only some-domain.com will be extracted. */
            String[] split = hostname.split("\\.");
            if (split.length > 1) {
                dnsAnswerBuilder.domain(split[split.length - 2] + "." + split[split.length - 1]);
            } else if (split.length == 1) {
                dnsAnswerBuilder.domain(hostname); // Domain is a single word with no dots.
            } else {
                dnsAnswerBuilder.domain(""); // Domain is blank.
            }
        }
    }

    public List<TxtDnsAnswer> txtLookup(String hostName) throws InterruptedException, ExecutionException {

        if (isShutdown()) {
            throw new DnsClientNotRunningException();
        }

        LOG.debug("Attempting to perform TXT lookup for hostname [{}]", hostName);

        validateHostName(hostName);

        DnsResponse content = null;
        try {
            content = resolver.query(new DefaultDnsQuestion(hostName, DnsRecordType.TXT)).sync().get().content();
            ArrayList<TxtDnsAnswer> txtRecords = new ArrayList<>();
            for (int i = 0; i < content.count(DnsSection.ANSWER); i++) {

                DnsRecord dnsRecord = content.recordAt(DnsSection.ANSWER, i);
                LOG.trace("TXT record [{}] retrieved with content [{}].", i, dnsRecord);

                if (dnsRecord instanceof DefaultDnsRawRecord) {
                    DefaultDnsRawRecord txtRecord = (DefaultDnsRawRecord) dnsRecord;

                    TxtDnsAnswer.Builder dnsAnswerBuilder = TxtDnsAnswer.builder();
                    String decodeTxtRecord = decodeTxtRecord(txtRecord);
                    LOG.trace("The decoded TXT record is [{}]", decodeTxtRecord);

                    dnsAnswerBuilder.value(decodeTxtRecord)
                                    .dnsTTL(txtRecord.timeToLive())
                                    .build();

                    txtRecords.add(dnsAnswerBuilder.build());
                }
            }

            return txtRecords;
        } finally {
            if (content != null) {
                // Must manually release references on content object since the DnsResponse class extends ReferenceCounted
                content.release();
            }
        }
    }

    private boolean isShutdown() {
        return nettyEventLoop == null || nettyEventLoop.isShutdown();
    }

    private static String decodeTxtRecord(DefaultDnsRawRecord record) {

        LOG.debug("Attempting to read TXT value from DNS record [{}]", record);

        return DefaultDnsRecordDecoder.decodeName(record.content());
    }

    public String getInverseAddressFormat(String ipAddress) {

        ipAddress = StringUtils.trim(ipAddress);

        validateIpAddress(ipAddress);

        LOG.debug("Preparing inverse format for IP address [{}]", ipAddress);

        // Detect what type of address is provided (IPv4 or IPv6)
        if (isIp6Address(ipAddress)) {

            LOG.debug("[{}] is an IPv6 address", ipAddress);

            /* Build reverse IPv6 address string with correct ip6.arpa suffix.
             * All hex nibbles from the full address should be reversed (with dots in between)
             * and the ip6.arpa suffix added to the end.
             *
             * For example, the reverse format for the address 2604:a880:800:10::7a1:b001 is
             * 1.0.0.b.1.a.7.0.0.0.0.0.0.0.0.0.0.1.0.0.0.0.8.0.0.8.8.a.4.0.6.2.ip6.arpa
             * See https://www.dnscheck.co/ptr-record-monitor for more info. */

            // Parse the full address as an InetAddress to allow the full address bytes (16 bytes/128 bits) to be obtained.
            byte[] addressBytes = InetAddresses.forString(ipAddress).getAddress();

            if (addressBytes.length > 16) {
                throw new IllegalArgumentException(String.format("[%s] is an invalid IPv6 address", ipAddress));
            }

            // Convert the raw address bytes to hex.
            char[] resolvedHex = new char[addressBytes.length * 2];
            for (int i = 0; i < addressBytes.length; i++) {
                int v = addressBytes[i] & 0xFF;
                resolvedHex[i * 2] = HEX_CHARS_ARRAY[v >>> 4];
                resolvedHex[i * 2 + 1] = HEX_CHARS_ARRAY[v & 0x0F];
            }

            String fullHexAddress = new String(resolvedHex).toLowerCase();
            String[] reversedAndSplit = new StringBuilder(fullHexAddress).reverse().toString().split("");

            String invertedAddress = Joiner.on(".").join(reversedAndSplit);

            LOG.debug("Inverted address [{}] built for [{}]", invertedAddress, ipAddress);

            return invertedAddress + IP_6_REVERSE_SUFFIX;
        } else {

            LOG.debug("[{}] is an IPv4 address", ipAddress);

            /* Build reverse IPv4 address string with correct in-addr.arpa suffix.
             * All octets should be reversed and the ip6.arpa suffix added to the end.
             *
             * For example, the reverse format for the address 10.20.30.40 is
             * 40.30.20.10.in-addr.arpa */
            final String[] octets = ipAddress.split("\\.");
            if (octets.length == 4) {
                String invertedAddress = octets[3] + "." + octets[2] + "." + octets[1] + "." + octets[0] + IP_4_REVERSE_SUFFIX;

                LOG.debug("Inverted address [{}] built for [{}]", invertedAddress, ipAddress);

                return invertedAddress;
            } else {
                throw new IllegalArgumentException(
                        String.format("[%s] is an invalid IPv4 address. " +
                                      "Please provide an address in the format 10.20.30.40", ipAddress));
            }
        }
    }

    private void validateHostName(String hostName) {

        if (!isHostName(hostName)) {
            throw new IllegalArgumentException(
                    String.format("[%s] is an invalid hostname. Please supply a pure hostname (eg. api.graylog.com)",
                                  hostName));
        }
    }

    public static boolean isHostName(String hostName) {

        return VALID_HOSTNAME_PATTERN.matcher(hostName).matches();
    }

    private static void validateIpAddress(final String ipAddress) {

        if (!isValidIpAddress(ipAddress)) {
            throw new IllegalArgumentException(
                    String.format("[%s] is an invalid IPv4 addresses.", ipAddress));
        }
    }

    private static boolean isValidIpAddress(String ipAddress) {

        return isIp4Address(ipAddress) || isIp6Address(ipAddress);
    }

    public static boolean isIp4Address(String ipAddress) {

        try {
            InetAddress address = InetAddresses.forString(ipAddress);
            if (address instanceof Inet4Address) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            // Absorb exception.
        }

        return false;
    }

    public static boolean isIp6Address(String ipAddress) {

        try {
            InetAddress address = InetAddresses.forString(ipAddress);
            if (address instanceof Inet6Address) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            // Absorb exception.
        }

        return false;
    }

    /**
     * @param ipAddresses A comma-separated list of IP addresses
     * @return true if all comma-separated IP addresses are valid
     * "8.8.4.4, 8.8.8.8" returns true
     * "8.8.4.4, " returns true
     * "8.8.4.4" returns true
     * "8.8.4.4 8.8.8.8" returns false
     * "8.8.4.4, google.com" returns false
     */
    public static boolean allIpAddressesValid(@Nullable String ipAddresses) {

        if (!Strings.isNullOrEmpty(ipAddresses)) {
            return Lists.newArrayList(Splitter.on(",")
                                              .trimResults()
                                              .omitEmptyStrings()
                                              .split(ipAddresses)).stream()
                        .map(hostAndPort -> HostAndPort.fromString(hostAndPort).withDefaultPort(DnsClient.DEFAULT_DNS_PORT))
                        .allMatch(hostAndPort -> isValidIpAddress(hostAndPort.getHost()));
        }

        return false;
    }
}