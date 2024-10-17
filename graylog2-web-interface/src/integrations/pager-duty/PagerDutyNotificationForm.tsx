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
import get from 'lodash/get';
import cloneDeep from 'lodash/cloneDeep';

import { Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';

type PagerDutyNotificationFormProps = {
  config: {
    client_name?: string;
    client_url?: string;
    custom_incident?: boolean;
    key_prefix?: string;
    routing_key?: string;
  };
  validation: {
    failed: boolean;
    errors?: {
      client_name?: string[];
      client_url?: string[];
      custom_incident?: string[];
      key_prefix?: string[];
      routing_key?: string[];
    };
    error_context?: any;
  };
  onChange: (...args: any[]) => void;
};

class PagerDutyNotificationForm extends React.Component<PagerDutyNotificationFormProps, {
  [key: string]: any;
}> {
  static defaultConfig = {
    client_name: 'Graylog',
    client_url: '',
    custom_incident: true,
    key_prefix: 'Graylog',
    routing_key: '',
  };

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event) => {
    const { name } = event.target;
    this.propagateChange(name, getValueFromInput(event.target));
  };

  render() {
    const { config, validation } = this.props;

    return (
      <>
        <Input id="pagerduty-notification-v2-routing_key"
               name="routing_key"
               label="Routing Key"
               type="text"
               bsStyle={validation.errors.routing_key ? 'error' : null}
               help={get(validation, 'errors.routing_key[0]', 'The Pager Duty integration Routing Key.')}
               value={config.routing_key}
               onChange={this.handleChange}
               required />
        <Input id="pagerduty-notification-v2-custom_incident"
               name="custom_incident"
               label="Use Custom Incident Key"
               type="checkbox"
               bsStyle={validation.errors.custom_incident ? 'error' : null}
               help={get(validation, 'errors.custom_incident[0]', 'Generate a custom incident key based on the Stream and the Alert Condition.')}
               checked={config.custom_incident}
               onChange={this.handleChange} />
        <Input id="pagerduty-notification-v2-key_prefix"
               name="key_prefix"
               label="Incident Key Prefix"
               type="text"
               bsStyle={validation.errors.key_prefix ? 'error' : null}
               help={get(validation, 'errors.key_prefix[0]', 'Incident key prefix that identifies the incident.')}
               value={config.key_prefix}
               onChange={this.handleChange}
               required />
        <Input id="pagerduty-notification-v2-client_name"
               name="client_name"
               label="Client Name"
               type="text"
               bsStyle={validation.errors.client_name ? 'error' : null}
               help={get(validation, 'errors.client_name[0]', 'The name of the Graylog system that is triggering the PagerDuty event.')}
               value={config.client_name}
               onChange={this.handleChange}
               required />
        <Input id="pagerduty-notification-v2-client_url"
               name="client_url"
               label="Client URL"
               type="text"
               bsStyle={validation.errors.client_url ? 'error' : null}
               help={get(validation, 'errors.client_url[0]', 'The URL of the Graylog system that is triggering the PagerDuty event.')}
               value={config.client_url}
               onChange={this.handleChange}
               required />
      </>
    );
  }
}

export default PagerDutyNotificationForm;
