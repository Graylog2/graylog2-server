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

import { Input } from 'components/bootstrap';
import { URLAllowListInput } from 'components/common';
import type { ValidationState } from 'components/common/types';

type DSVHTTPAdapterFieldSetProps = {
  config: any;
  handleFormEvent: (...args: any[]) => void;
  validationState: (type: string) => ValidationState;
  validationMessage: (type: string, message: string) => ValidationState;
};

const DSVHTTPAdapterFieldSet = ({
  handleFormEvent,
  validationState,
  validationMessage,
  config,
}: DSVHTTPAdapterFieldSetProps) => {
  const { t } = useTranslation('lookup_tables');

  return (
    <fieldset>
      <URLAllowListInput
        label={t('dsv_adapter.file_url')}
        onChange={handleFormEvent}
        validationMessage={validationMessage('url', t('dsv_adapter.file_url_help'))}
        validationState={validationState('url')}
        url={config.url}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="number"
        id="refresh_interval"
        name="refresh_interval"
        label={t('dsv_adapter.refresh_interval')}
        required
        onChange={handleFormEvent}
        help={t('dsv_adapter.refresh_interval_help')}
        value={config.refresh_interval}
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
        help={t('dsv_adapter.separator_help')}
        value={config.separator}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="line_separator"
        name="line_separator"
        label={t('adapter_fields.line_separator')}
        required
        onChange={handleFormEvent}
        help={t('dsv_adapter.line_separator_help')}
        value={config.line_separator}
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
        id="ignorechar"
        name="ignorechar"
        label={t('dsv_adapter.ignore_characters')}
        required
        onChange={handleFormEvent}
        help={t('dsv_adapter.ignore_characters_help')}
        value={config.ignorechar}
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
        help={t('dsv_adapter.key_column_help')}
        value={config.key_column}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="value_column"
        name="value_column"
        label={t('adapter_fields.value_column')}
        required
        onChange={handleFormEvent}
        help={t('dsv_adapter.value_column_help')}
        value={config.value_column}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
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
        id="check_presence_only"
        name="check_presence_only"
        label={t('dsv_adapter.presence_only')}
        checked={config.check_presence_only}
        onChange={handleFormEvent}
        help={t('dsv_adapter.presence_only_help')}
        wrapperClassName="col-md-offset-3 col-md-9"
      />
    </fieldset>
  );
};

export default DSVHTTPAdapterFieldSet;
