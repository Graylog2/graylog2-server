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
import cloneDeep from 'lodash/cloneDeep';
import { useTranslation } from 'react-i18next';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import type { ValidationState } from 'components/common/types';

const OTX_INDICATORS = [
  { label: 'otx_adapter_field_set.option_ip_auto_detect', value: 'IPAutoDetect' },
  { label: 'otx_adapter_field_set.option_ip_v4', value: 'IPv4' },
  { label: 'otx_adapter_field_set.option_ip_v6', value: 'IPv6' },
  { label: 'otx_adapter_field_set.option_domain', value: 'domain' },
  { label: 'otx_adapter_field_set.option_hostname', value: 'hostname' },
  { label: 'otx_adapter_field_set.option_file', value: 'file' },
  { label: 'otx_adapter_field_set.option_url', value: 'url' },
  { label: 'otx_adapter_field_set.option_cve', value: 'cve' },
  { label: 'otx_adapter_field_set.option_nids', value: 'nids' },
  { label: 'otx_adapter_field_set.option_correlation_rule', value: 'correlation-rule' },
];

type Props = {
  config: {
    indicator: string;
    api_key?: string;
    api_url: string;
    http_user_agent: string;
    http_connect_timeout: number;
    http_write_timeout: number;
    http_read_timeout: number;
  };
  updateConfig: (...args: any[]) => void;
  handleFormEvent: (...args: any[]) => void;
  validationState: (...args: any[]) => ValidationState;
  validationMessage: (...args: any[]) => React.ReactElement | string;
};

const OTXAdapterFieldSet = ({ updateConfig, config, handleFormEvent, validationState, validationMessage }: Props) => {
  const { t } = useTranslation('integrations');
  const handleSelect = (fieldName: string) => (selectedIndicator: string) => {
    const newConfig = cloneDeep(config);
    newConfig[fieldName] = selectedIndicator;
    updateConfig(newConfig);
  };

  return (
    <fieldset>
      <Input
        id="indicator"
        label={t('otx_adapter_field_set.indicator_label')}
        required
        onChange={handleFormEvent}
        help={validationMessage('indicator', t('otx_adapter_field_set.indicator_help'))}
        bsStyle={validationState('indicator')}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9">
        <Select
          placeholder={t('otx_adapter_field_set.indicator_placeholder')}
          clearable={false}
          options={OTX_INDICATORS.map((indicator) => ({ ...indicator, label: t(indicator.label) }))}
          onChange={handleSelect('indicator')}
          value={config.indicator}
        />
      </Input>
      <Input
        type="text"
        id="api_key"
        name="api_key"
        label={t('otx_adapter_field_set.api_key_label')}
        onChange={handleFormEvent}
        help={validationMessage('api_key', t('otx_adapter_field_set.api_key_help'))}
        bsStyle={validationState('api_key')}
        value={config.api_key}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="api_url"
        name="api_url"
        label={t('otx_adapter_field_set.api_url_label')}
        onChange={handleFormEvent}
        help={validationMessage('api_url', t('otx_adapter_field_set.api_url_help'))}
        bsStyle={validationState('api_url')}
        value={config.api_url}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="text"
        id="http_user_agent"
        name="http_user_agent"
        label={t('otx_adapter_field_set.user_agent_label')}
        required
        onChange={handleFormEvent}
        help={validationMessage('http_user_agent', t('otx_adapter_field_set.user_agent_help'))}
        bsStyle={validationState('http_user_agent')}
        value={config.http_user_agent}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="number"
        id="http_connect_timeout"
        name="http_connect_timeout"
        label={t('otx_adapter_field_set.connect_timeout_label')}
        required
        onChange={handleFormEvent}
        help={validationMessage('http_connect_timeout', t('otx_adapter_field_set.connect_timeout_help'))}
        bsStyle={validationState('http_connect_timeout')}
        value={config.http_connect_timeout}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="number"
        id="http_write_timeout"
        name="http_write_timeout"
        label={t('otx_adapter_field_set.write_timeout_label')}
        required
        onChange={handleFormEvent}
        help={validationMessage('http_write_timeout', t('otx_adapter_field_set.write_timeout_help'))}
        bsStyle={validationState('http_write_timeout')}
        value={config.http_write_timeout}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="number"
        id="http_read_timeout"
        name="http_read_timeout"
        label={t('otx_adapter_field_set.read_timeout_label')}
        required
        onChange={handleFormEvent}
        help={validationMessage('http_read_timeout', t('otx_adapter_field_set.read_timeout_help'))}
        bsStyle={validationState('http_read_timeout')}
        value={config.http_read_timeout}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
    </fieldset>
  );
};

export default OTXAdapterFieldSet;
