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

import { Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import useProductName from 'brand-customization/useProductName';

import type { PagerDutyConfig } from './PagerDutyConfig';

type PagerDutyNotificationFormProps = {
  config: PagerDutyConfig;
  validation: {
    failed: boolean;
    errors?: {
      client_name?: string[];
      client_url?: string[];
      custom_incident?: string[];
      key_prefix?: string[];
      routing_key?: string[];
      pager_duty_title?: string[];
      incident_key?: string[];
    };
    error_context?: any;
  };
  onChange: (...args: any[]) => void;
};

const PagerDutyNotificationForm = ({ config, validation, onChange }: PagerDutyNotificationFormProps) => {
  const productName = useProductName();
  const propagateChange = (key: string, value: unknown) => {
    const nextConfig = cloneDeep(config);
    if ((key === 'pager_duty_title' || key === 'incident_key') && value === '') {
      nextConfig[key] = null;
    } else {
      nextConfig[key] = value;
    }
    onChange(nextConfig);
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name } = event.target;
    propagateChange(name, getValueFromInput(event.target));
  };

  return (
    <>
      <Input
        id="pagerduty-notification-v2-routing_key"
        name="routing_key"
        label="Routing Key"
        type="text"
        bsStyle={validation.errors.routing_key ? 'error' : null}
        help={validation.errors?.routing_key?.[0] ?? 'The Pager Duty integration Routing Key.'}
        value={config.routing_key}
        onChange={handleChange}
        required
      />
      <Input
        id="pagerduty-notification-v2-pager_duty_title"
        name="pager_duty_title"
        label="Incident Title"
        type="text"
        bsStyle={validation.errors.pager_duty_title ? 'error' : null}
        help={
          validation.errors?.pager_duty_title?.[0] ??
          `Custom title for the incident in Pager Duty. Will be the event title as shown in ${productName} if not set.`
        }
        value={config.pager_duty_title}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-custom_incident"
        name="custom_incident"
        label="Use Custom Incident Key"
        type="checkbox"
        bsStyle={validation.errors.custom_incident ? 'error' : null}
        help={validation.errors?.custom_incident?.[0] ?? 'Generate a custom incident key.'}
        checked={config.custom_incident}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-key_prefix"
        name="key_prefix"
        label="Incident Key Prefix"
        type="text"
        bsStyle={validation.errors.key_prefix ? 'error' : null}
        help={
          validation.errors?.key_prefix?.[0] ??
          'Incident key prefix that will be followed by Stream(s) and the Event Definition title. Use Incident Key to customize entire key.'
        }
        value={config.key_prefix}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-incident_key"
        name="incident_key"
        label="Incident Key"
        type="text"
        bsStyle={validation.errors.incident_key ? 'error' : null}
        help={
          validation.errors?.incident_key?.[0] ??
          'Full key to identify the incident. Will be used instead of prefix, if provided.'
        }
        value={config.incident_key}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-client_name"
        name="client_name"
        label="Client Name"
        type="text"
        bsStyle={validation.errors.client_name ? 'error' : null}
        help={
          validation.errors?.client_name?.[0] ??
          `The name of the ${productName} system that is triggering the PagerDuty event.`
        }
        value={config.client_name}
        onChange={handleChange}
        required
      />
      <Input
        id="pagerduty-notification-v2-client_url"
        name="client_url"
        label="Client URL"
        type="text"
        bsStyle={validation.errors.client_url ? 'error' : null}
        help={
          validation.errors?.client_url?.[0] ??
          `The URL of the ${productName} system that is triggering the PagerDuty event.`
        }
        value={config.client_url}
        onChange={handleChange}
        required
      />
    </>
  );
};

export default PagerDutyNotificationForm;
