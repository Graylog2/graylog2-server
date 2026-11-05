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
import type { SyntheticEvent } from 'react';
import React from 'react';
import { useTranslation } from 'react-i18next';

import { Input } from 'components/bootstrap';
import type { LookupTableDataAdapterConfig } from 'logic/lookup-tables/types';

type Props = {
  config: LookupTableDataAdapterConfig;
  handleFormEvent: (event: SyntheticEvent<EventTarget>) => void;
  validationState: (state: string) => 'error' | 'warning' | 'success';
  validationMessage: (field: string, message: string) => string;
};

const CSVFileAdapterFieldSet = ({ config, handleFormEvent, validationState, validationMessage }: Props) => {
  const { t } = useTranslation('lookup_tables');
  const valueLabel = config.multi_value_lookup ? t('adapter_fields.value_columns') : t('adapter_fields.value_column');
  const valueHelp = config.multi_value_lookup
    ? t('csv_adapter.value_columns_help')
    : t('csv_adapter.value_column_help');

  return (
    <fieldset>
      <Input
        type="text"
        id="path"
        name="path"
        label={t('csv_adapter.file_path')}
        autoFocus
        required
        onChange={handleFormEvent}
        help={validationMessage('path', t('csv_adapter.file_path_help'))}
        bsStyle={validationState('path')}
        value={config.path}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="number"
        id="check_interval"
        name="check_interval"
        label={t('adapter_fields.check_interval')}
        required
        onChange={handleFormEvent}
        help={t('csv_adapter.check_interval_help')}
        value={config.check_interval}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="separator"
        name="separator"
        label={t('adapter_fields.separator')}
        required
        onChange={handleFormEvent}
        help={t('csv_adapter.separator_help')}
        value={config.separator}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="quotechar"
        name="quotechar"
        label={t('adapter_fields.quote_character')}
        required
        onChange={handleFormEvent}
        help={t('adapter_fields.quote_help')}
        value={config.quotechar}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="key_column"
        name="key_column"
        label={t('adapter_fields.key_column')}
        required
        onChange={handleFormEvent}
        help={t('csv_adapter.key_column_help')}
        value={config.key_column}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="value_column"
        name="value_column"
        label={valueLabel}
        required={!config.multi_value_lookup}
        onChange={handleFormEvent}
        help={valueHelp}
        value={config.value_column}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="checkbox"
        id="multi_value_lookup"
        name="multi_value_lookup"
        label={t('adapter_fields.multi_value_lookup')}
        checked={config.multi_value_lookup}
        onChange={handleFormEvent}
        help={t('csv_adapter.multi_value_help')}
        wrapperClassName="col-md-offset-3 col-md-9"
      />
      <Input
        type="checkbox"
        id="case_insensitive_lookup"
        name="case_insensitive_lookup"
        label={t('adapter_fields.case_insensitive_label')}
        checked={config.case_insensitive_lookup}
        onChange={handleFormEvent}
        help={t('adapter_fields.case_insensitive_help')}
        wrapperClassName="col-md-offset-3 col-md-9"
      />
      <Input
        type="checkbox"
        id="cidr_lookup"
        name="cidr_lookup"
        label={t('adapter_fields.cidr_lookup')}
        checked={config.cidr_lookup}
        onChange={handleFormEvent}
        help={t('csv_adapter.cidr_help')}
        wrapperClassName="col-md-offset-3 col-md-9"
      />
    </fieldset>
  );
};

export default CSVFileAdapterFieldSet;
