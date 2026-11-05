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
/* eslint-disable react/no-unescaped-entities */
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { MantineAccordion } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

const EXAMPLE_IPADDR = 'ipaddr';
const EXAMPLE_HOSTNAME = 'hostname';
const EXAMPLE_USER_ID = 'user_id';
const EXAMPLE_FIRST_NAME_LAST_NAME_USERNAME = 'first_name,last_name,username';
const EXAMPLE_TRUE = 'true';
const EXAMPLE_CIDR = 'cidr';
const EXAMPLE_SUBNET = 'subnet';

const csvFile1 = `"ipaddr","hostname"
"127.0.0.1","localhost"
"10.0.0.1","server1"
"10.0.0.2","server2"`;

const csvFile2 = `'ipaddr';'lladdr';'hostname'
'127.0.0.1';'e4:b2:11:d1:38:14';'localhost'
'10.0.0.1';'e4:b2:12:d1:48:28';'server1'
'10.0.0.2';'e4:b2:11:d1:58:34';'server2'`;

const csvFile3 = `"cidr","subnet"
"192.168.100.0/24","Finance Department subnet"
"192.168.101.0/24","IT Department subnet"
"192.168.102.0/24","HR Department subnet"`;

const multiValueCsvFile = `"user_id","first_name","last_name","username","email"
"000001","User","One","user1","user.one@company.net"
"000002","User","Two","user2","u2@company.net"
"000003","Admin","One","admin1","admin@company.net"`;

const CIDR_EXAMPLE_KEY = '192.168.101.64';
const CIDR_EXAMPLE_RESULT = 'IT Department subnet';

const CSVFileAdapterDocumentation = () => {
  const { t } = useTranslation('lookup_tables');
  const productName = useProductName();

  const accordionItems = [
    {
      value: 'example_1',
      label: t('doc.example', { number: 1 }),
      content: (
        <div>
          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('adapter_fields.separator')}: <code>,</code>
            <br />
            {t('adapter_fields.quote_character')}: <code>"</code>
            <br />
            {t('adapter_fields.key_column')}: <code>{EXAMPLE_IPADDR}</code>
            <br />
            {t('adapter_fields.value_column')}: <code>{EXAMPLE_HOSTNAME}</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>{t('csv_doc.csv_file')}</h5>
          <pre>{csvFile1}</pre>
        </div>
      ),
    },
    {
      value: 'example_2',
      label: t('doc.example', { number: 2 }),
      content: (
        <div>
          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('adapter_fields.separator')}: <code>;</code>
            <br />
            {t('adapter_fields.quote_character')}: <code>'</code>
            <br />
            {t('adapter_fields.key_column')}: <code>{EXAMPLE_IPADDR}</code>
            <br />
            {t('adapter_fields.value_column')}: <code>{EXAMPLE_HOSTNAME}</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>{t('csv_doc.csv_file')}</h5>
          <pre>{csvFile2}</pre>
        </div>
      ),
    },
    {
      value: 'multi_value_example',
      label: t('csv_doc.multi_value_example'),
      content: (
        <div>
          <h3 style={{ marginBottom: 10 }}>{t('csv_doc.multi_value_heading')}</h3>

          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('adapter_fields.separator')}: <code>,</code>
            <br />
            {t('adapter_fields.quote_character')}: <code>"</code>
            <br />
            {t('adapter_fields.key_column')}: <code>{EXAMPLE_USER_ID}</code>
            <br />
            {t('adapter_fields.value_columns')}: <code>{EXAMPLE_FIRST_NAME_LAST_NAME_USERNAME}</code>
            <br />
            {t('adapter_fields.multi_value_lookup')}: <code>{EXAMPLE_TRUE}</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>{t('csv_doc.csv_file')}</h5>
          <pre>{multiValueCsvFile}</pre>
        </div>
      ),
    },
    {
      value: 'cidr_lookups',
      label: t('csv_doc.cidr_lookups'),
      content: (
        <div>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('csv_doc.cidr_intro_1')}
            <br />
            {t('csv_doc.cidr_intro_2')}
            <br />
          </p>

          <h5 style={{ marginBottom: 10 }}>{t('shared.configuration')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('adapter_fields.separator')}: <code>,</code>
            <br />
            {t('adapter_fields.quote_character')}: <code>"</code>
            <br />
            {t('adapter_fields.key_column')}: <code>{EXAMPLE_CIDR}</code>
            <br />
            {t('adapter_fields.value_column')}: <code>{EXAMPLE_SUBNET}</code>
            <br />
            {t('adapter_fields.cidr_lookup')}: <code>{EXAMPLE_TRUE}</code>
          </p>

          <h5 style={{ marginBottom: 10 }}>{t('csv_doc.csv_file')}</h5>
          <pre>{csvFile3}</pre>

          <p>{t('csv_doc.cidr_result', { key: CIDR_EXAMPLE_KEY, result: CIDR_EXAMPLE_RESULT })}</p>
        </div>
      ),
    },
  ];

  return (
    <div>
      <p>{t('csv_doc.intro')}</p>
      <p>{t('csv_doc.formatting_hint')}</p>

      <h4>{t('doc.requirements_title')}</h4>
      <p>{t('csv_doc.first_line')}</p>
      <p>
        <Trans t={t} i18nKey="doc.utf8" components={{ bold: <strong /> }} />
      </p>
      <p>
        <Trans t={t} i18nKey="csv_doc.readable" values={{ productName }} components={{ bold: <strong /> }} />
      </p>

      <hr />

      <MantineAccordion defaultValue="example_1" accordionItems={accordionItems} />
    </div>
  );
};

export default CSVFileAdapterDocumentation;
