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

type DSVHTTPAdapterConfig = {
  url: string;
  separator: string;
  line_separator: string;
  quotechar: string;
  ignorechar: string;
  key_column: string;
  value_column: string;
  check_interval: number;
  case_insensitive_lookup: boolean;
};

type Props = {
  dataAdapter: {
    config: DSVHTTPAdapterConfig;
  };
};

const DSVHTTPAdapterSummary = ({ dataAdapter }: Props) => {
  const { t } = useTranslation('lookup_tables');
  const { config } = dataAdapter;

  return (
    <dl>
      <dt>{t('dsv_adapter.file_url')}</dt>
      <dd>{config.url}</dd>
      <dt>{t('adapter_fields.separator')}</dt>
      <dd>
        <code>{config.separator}</code>
      </dd>
      <dt>{t('adapter_fields.line_separator')}</dt>
      <dd>
        <code>{config.line_separator}</code>
      </dd>
      <dt>{t('adapter_fields.quote_character')}</dt>
      <dd>
        <code>{config.quotechar}</code>
      </dd>
      <dt>{t('dsv_adapter.ignore_lines_starting_with')}</dt>
      <dd>
        <code>{config.ignorechar}</code>
      </dd>
      <dt>{t('adapter_fields.key_column')}</dt>
      <dd>{config.key_column}</dd>
      <dt>{t('adapter_fields.value_column')}</dt>
      <dd>{config.value_column}</dd>
      <dt>{t('adapter_fields.check_interval')}</dt>
      <dd>{t('adapter_fields.seconds', { count: config.check_interval })}</dd>
      <dt>{t('adapter_fields.case_insensitive_summary')}</dt>
      <dd>{config.case_insensitive_lookup ? t('shared.yes') : t('shared.no')}</dd>
    </dl>
  );
};

export default DSVHTTPAdapterSummary;
