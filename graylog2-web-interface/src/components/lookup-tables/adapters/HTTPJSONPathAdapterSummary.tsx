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
import { useTranslation } from 'react-i18next';

import { KeyValueTable } from 'components/common';

type HTTPJSONPathAdapterSummaryProps = {
  dataAdapter: any;
};

const HTTPJSONPathAdapterSummary = ({ dataAdapter }: HTTPJSONPathAdapterSummaryProps) => {
  const { t } = useTranslation('lookup_tables');
  const { config } = dataAdapter;

  return (
    <dl>
      <dt>{t('http_doc.lookup_url_label')}</dt>
      <dd>{config.url}</dd>
      <dt>{t('http_doc.single_value_label')}</dt>
      <dd>
        <code>{config.single_value_jsonpath}</code>
      </dd>
      <dt>{t('http_doc.multi_value_label')}</dt>
      <dd>
        <code>{config.multi_value_jsonpath}</code>
      </dd>
      <dt>{t('http_doc.http_useragent_label')}</dt>
      <dd>{config.user_agent}</dd>
      <dt>{t('http_fields.headers_label')}</dt>
      <dd>
        <KeyValueTable pairs={config.headers || {}} />
      </dd>
    </dl>
  );
};

export default HTTPJSONPathAdapterSummary;
