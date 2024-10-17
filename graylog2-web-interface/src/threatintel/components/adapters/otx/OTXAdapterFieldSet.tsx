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
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import type { ValidationState } from 'components/common/types';

const OTX_INDICATORS = [
  { label: 'IP Auto-Detect', value: 'IPAutoDetect' },
  { label: 'IP v4', value: 'IPv4' },
  { label: 'IP v6', value: 'IPv6' },
  { label: 'Domain', value: 'domain' },
  { label: 'Hostname', value: 'hostname' },
  { label: 'File', value: 'file' },
  { label: 'URL', value: 'url' },
  { label: 'CVE', value: 'cve' },
  { label: 'NIDS', value: 'nids' },
  { label: 'Correlation-Rule', value: 'correlation-rule' },
];

type OTXAdapterFieldSetProps = {
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

class OTXAdapterFieldSet extends React.Component<OTXAdapterFieldSetProps, {
  [key: string]: any;
}> {
  handleSelect = (fieldName) => (selectedIndicator) => {
    const config = lodash.cloneDeep(this.props.config);
    config[fieldName] = selectedIndicator;
    this.props.updateConfig(config);
  };

  render() {
    const { config } = this.props;

    return (
      <fieldset>
        <Input id="indicator"
               label="Indicator"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('indicator', 'The OTX indicator type that should be used for lookups.')}
               bsStyle={this.props.validationState('indicator')}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select indicator"
                  clearable={false}
                  options={OTX_INDICATORS}
                  matchProp="label"
                  onChange={this.handleSelect('indicator')}
                  value={config.indicator} />
        </Input>
        <Input type="text"
               id="api_key"
               name="api_key"
               label="OTX API Key"
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('api_key', 'Your OTX API key.')}
               bsStyle={this.props.validationState('api_key')}
               value={config.api_key}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="text"
               id="api_url"
               name="api_url"
               label="OTX API URL"
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('api_url', 'URL of the OTX API server.')}
               bsStyle={this.props.validationState('api_url')}
               value={config.api_url}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="text"
               id="http_user_agent"
               name="http_user_agent"
               label="HTTP User-Agent"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('http_user_agent', 'The User-Agent header that should be used for the HTTP request.')}
               bsStyle={this.props.validationState('http_user_agent')}
               value={config.http_user_agent}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="number"
               id="http_connect_timeout"
               name="http_connect_timeout"
               label="HTTP Connect Timeout"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('http_connect_timeout', 'HTTP connection timeout in milliseconds.')}
               bsStyle={this.props.validationState('http_connect_timeout')}
               value={config.http_connect_timeout}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="number"
               id="http_write_timeout"
               name="http_write_timeout"
               label="HTTP Write Timeout"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('http_write_timeout', 'HTTP write timeout in milliseconds.')}
               bsStyle={this.props.validationState('http_write_timeout')}
               value={config.http_write_timeout}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="number"
               id="http_read_timeout"
               name="http_read_timeout"
               label="HTTP Read Timeout"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('http_read_timeout', 'HTTP read timeout in milliseconds.')}
               bsStyle={this.props.validationState('http_read_timeout')}
               value={config.http_read_timeout}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
      </fieldset>
    );
  }
}

export default OTXAdapterFieldSet;
