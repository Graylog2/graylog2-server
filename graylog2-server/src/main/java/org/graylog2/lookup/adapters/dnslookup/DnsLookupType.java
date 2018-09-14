package org.graylog2.lookup.adapters.dnslookup;

public enum DnsLookupType {

    A,
    AAAA,
    A_AAAA, // Performs both A and AAAA lookup
    PTR,
    TXT
}
