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

import type { LookupTableAdapter } from 'logic/lookup-tables/types';

type Props = {
  dataAdapter: LookupTableAdapter;
};

const CSVFileAdapterSummary = ({ dataAdapter }: Props) => {
  const { t } = useTranslation('lookup_tables');
  const { config } = dataAdapter;
  const valueLabel = config.multi_value_lookup ? t('adapter_fields.value_columns') : t('adapter_fields.value_column');
  const value =
    config.multi_value_lookup && !config.value_column ? t('csv_adapter.all_columns_except_key') : config.value_column;

  return (
    <dl>
      <dt>{t('csv_adapter.file_path')}</dt>
      <dd>{config.path}</dd>
      <dt>{t('adapter_fields.separator')}</dt>
      <dd>
        <code>{config.separator}</code>
      </dd>
      <dt>{t('adapter_fields.quote_character')}</dt>
      <dd>
        <code>{config.quotechar}</code>
      </dd>
      <dt>{t('adapter_fields.key_column')}</dt>
      <dd>{config.key_column}</dd>
      <dt>{valueLabel}</dt>
      <dd>{value}</dd>
      <dt>{t('adapter_fields.check_interval')}</dt>
      <dd>{t('adapter_fields.seconds', { count: config.check_interval })}</dd>
      <dt>{t('adapter_fields.multi_value_lookup')}</dt>
      <dd>{config.multi_value_lookup ? t('shared.yes') : t('shared.no')}</dd>
      <dt>{t('adapter_fields.case_insensitive_summary')}</dt>
      <dd>{config.case_insensitive_lookup ? t('shared.yes') : t('shared.no')}</dd>
      <dt>{t('adapter_fields.cidr_lookup')}</dt>
      <dd>{config.cidr_lookup ? t('shared.yes') : t('shared.no')}</dd>
    </dl>
  );
};

export default CSVFileAdapterSummary;
