/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { MantineAccordion } from 'components/bootstrap';

const StyledPre = styled.pre(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
  `,
);

const DnsAdapterDocumentation = () => {
  const { t } = useTranslation('lookup_tables');
  const styleMarginBottom = { marginBottom: 10 };

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

  const accordionItems = [
    {
      value: 'ipv4',
      label: t('dns_adapter.lookup_type_a'),
      content: (
        <div>
          <p style={styleMarginBottom}>
            <Trans t={t} i18nKey="dns_doc.a" components={{ code: <code /> }} />
          </p>
          <StyledPre>{aResponse}</StyledPre>
        </div>
      ),
    },
    {
      value: 'ipv6',
      label: t('dns_adapter.lookup_type_aaaa'),
      content: (
        <div>
          <p style={styleMarginBottom}>
            <Trans t={t} i18nKey="dns_doc.aaaa" components={{ code: <code /> }} />
          </p>
          <StyledPre>{aaaaResponse}</StyledPre>
        </div>
      ),
    },
    {
      value: 'ipv4&ipv6',
      label: t('dns_adapter.lookup_type_a_aaaa_singular'),
      content: (
        <div>
          <p style={styleMarginBottom}>
            <Trans t={t} i18nKey="dns_doc.a_aaaa" components={{ code: <code /> }} />
          </p>
          <StyledPre>{aAndAaaaResponse}</StyledPre>
        </div>
      ),
    },
    {
      value: 'ptr',
      label: t('dns_adapter.lookup_type_ptr'),
      content: (
        <div>
          <p style={styleMarginBottom}>
            <Trans t={t} i18nKey="dns_doc.ptr" components={{ code: <code /> }} />
          </p>
          <StyledPre>{ptrResponse}</StyledPre>
        </div>
      ),
    },
    {
      value: 'txt',
      label: t('dns_adapter.lookup_type_txt'),
      content: (
        <div>
          <p style={styleMarginBottom}>
            <Trans t={t} i18nKey="dns_doc.txt" components={{ code: <code /> }} />
          </p>
          <StyledPre>{txtResponse}</StyledPre>
        </div>
      ),
    },
  ];

  return (
    <div>
      <h3 style={styleMarginBottom}>{t('shared.configuration')}</h3>

      <h5 style={styleMarginBottom}>{t('dns_adapter.lookup_type_label')}</h5>

      <MantineAccordion accordionItems={accordionItems} defaultValue="ipv4" />

      <hr />
      <h5 style={styleMarginBottom}>{t('dns_doc.server_ips_title')}</h5>

      <p style={styleMarginBottom}>
        <Trans t={t} i18nKey="dns_doc.server_ips" components={{ code: <code /> }} />
      </p>

      <h5 style={styleMarginBottom}>{t('dns_adapter.request_timeout_label')}</h5>

      <p style={styleMarginBottom}>{t('dns_doc.request_timeout')}</p>

      <h5 style={styleMarginBottom}>{t('dns_adapter.cache_ttl_override')}</h5>

      <p style={styleMarginBottom}>{t('dns_doc.cache_ttl_override')}</p>
    </div>
  );
};

export default DnsAdapterDocumentation;
