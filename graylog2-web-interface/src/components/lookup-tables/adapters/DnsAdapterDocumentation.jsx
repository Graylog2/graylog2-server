import React from 'react';

const DnsAdapterDocumentation = () => {
  const styleMarginBottom = { marginBottom: 10 };
  const codeStyle = { fontSize: 10 };

  const aResponse = `{
  "single_value": "34.239.63.98",
  "multi_value": {
    "results": [
      {
        "ip_address": "34.239.63.98",
        "dns_ttl": 60
      },
      {
        "ip_address": "34.238.48.57",
        "dns_ttl": 60
      }
    ]
  },
  "ttl": 60000
}`;

  const aaaaResponse = `{
  "single_value": "2307:f8b0:3000:800:0:0:0:200e",
  "multi_value": {
    "results": [
      {
        "ip_address": "2307:f8b0:3000:800:0:0:0:200e",
        "dns_ttl": 77
      }
    ]
  },
  "ttl": 77000
}`;

  const aAndAaaaResponse = `{
  "single_value": "144.222.6.132",
  "multi_value": {
    "results": [
      {
        "ip_address": "144.222.6.132",
        "dns_ttl": 32,
        "ip_version": "IPv4"
      },
      {
        "ip_address": "1207:f8b1:6003:b01:0:0:0:8a",
        "dns_ttl": 299,
        "ip_version": "IPv6"
      }
    ]
  },
  "ttl": 32000
}`;

  const ptrResponse = `{
  "single_value": "c-45-216-65-41.hd1.fl.someisp.co.uk",
  "multi_value": {
    "domain": "someisp.co.uk",
    "full_domain": "c-45-216-65-41.hd1.fl.someisp.co.uk",
    "dns_ttl": "300",
  },
  "ttl": 300000
}`;

  const txtResponse = `{
  "single_value": null,
  "multi_value": {
    "results": [
      {
        "value": "Some text value that lives in a TXT DNS",
        "dns_ttl": 300
      },
      {
        "value": "v=spf1 include:some-email-domain.org ~all.",
        "dns_ttl": 200
      }
    ]
  },
  "ttl": 200000
}`;

  return (


    <div>

      <h3 style={styleMarginBottom}>Configuration</h3>

      <h5 style={styleMarginBottom}>DNS Lookup Type</h5>

      <p style={styleMarginBottom}>
        <strong>Resolve hostname to IPv4 address (A)</strong>: Returns both a <code>single_value</code> containing one
        of the IPv4 addresses that the hostname resolves to,
        and a <code>multi_value</code> containing all IPv4 addresses that the hostname resolves to.
        Input for this type must be a pure domain name (eg. <code>api.graylog.com</code>).
      </p>
      <pre style={codeStyle}>{aResponse}</pre>

      <p style={styleMarginBottom}>
        <strong>Resolve hostname to IPv6 address (AAAA)</strong>: Returns both a <code>single_value</code> containing
        one of the IPv6 addresses that the hostname resolves to,
        and a <code>multi_value</code> containing all IPv6 addresses that the hostname resolves to.
        Input for this type must be a pure domain name (eg. <code>api.graylog.com</code>).
      </p>
      <pre style={codeStyle}>{aaaaResponse}</pre>

      <p style={styleMarginBottom}>
        <strong>Resolve hostname to IPv4 and IPv6 address (A and AAAA)</strong>: Returns both
        a <code>single_value</code> containing
        one of the IPv4 or IPv6 addresses that the hostname resolves to (will return IPv4 if available),
        and a <code>multi_value</code> containing all IPv4 and IPv6 addresses that the hostname resolves to.
        Input for this type must be a pure domain name (eg. <code>api.graylog.com</code>).
      </p>
      <pre style={codeStyle}>{aAndAaaaResponse}</pre>

      <p style={styleMarginBottom}>
        <strong>Reverse lookup (PTR)</strong>: Returns a <code>single_value</code> containing the PTR value if defined
        for the IP address. The <code>domain</code> field displays the domain name (with no subdomains).
        The <code>full_domain</code> field displays the full un-trimmed host name/PTR value.
        The input for this type must be a pure IPv4 or IPv6 address
        (eg. <code>10.0.0.1</code> or <code>2622:f3b0:4000:812::200c</code>).
      </p>
      <pre style={codeStyle}>{ptrResponse}</pre>

      <p style={styleMarginBottom}>
        <strong>Text lookup (TXT)</strong>: Returns a <code>multi_value</code> with all TXT records defined for the
        hostname.
        Input for this type must be a pure domain name (eg. <code>api.graylog.com</code>).
      </p>
      <pre style={codeStyle}>{txtResponse}</pre>

      <h5 style={styleMarginBottom}>DNS Server IP Addresses</h5>

      <p style={styleMarginBottom}>
        A comma-separated list of DNS server IP addresses and optional ports to use (eg. <code>192.168.1.1:5353,
          192.168.1.244
                                                                                         </code>).
        Leave this blank to use the DNS server defined for your local system. All requests use port 53 unless
        otherwise specified.
      </p>

      <h5 style={styleMarginBottom}>DNS Request Timeout</h5>

      <p style={styleMarginBottom}>
        The DNS request timeout in milliseconds.
      </p>

      <h5 style={styleMarginBottom}>Cache TTL Override</h5>

      <p style={styleMarginBottom}>
        If enabled, the TTL for this adapter&apos;s cache will be overridden with the specified value.
      </p>

    </div>
  );
};

export default DnsAdapterDocumentation;
