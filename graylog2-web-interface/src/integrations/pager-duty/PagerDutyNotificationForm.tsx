import React from 'react';
import get from 'lodash/get';
import cloneDeep from 'lodash/cloneDeep';
import useProductName from 'brand-customization/useProductName';

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

const PagerDutyNotificationForm = ({ config, validation, onChange }: PagerDutyNotificationFormProps) => {
  const productName = useProductName();
  const propagateChange = (key: string, value: unknown) => {
    const nextConfig = cloneDeep(config);
    nextConfig[key] = value;
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
        help={get(validation, 'errors.routing_key[0]', 'The Pager Duty integration Routing Key.')}
        value={config.routing_key}
        onChange={handleChange}
        required
      />
      <Input
        id="pagerduty-notification-v2-custom_incident"
        name="custom_incident"
        label="Use Custom Incident Key"
        type="checkbox"
        bsStyle={validation.errors.custom_incident ? 'error' : null}
        help={get(
          validation,
          'errors.custom_incident[0]',
          'Generate a custom incident key based on the Stream and the Alert Condition.',
        )}
        checked={config.custom_incident}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-key_prefix"
        name="key_prefix"
        label="Incident Key Prefix"
        type="text"
        bsStyle={validation.errors.key_prefix ? 'error' : null}
        help={get(validation, 'errors.key_prefix[0]', 'Incident key prefix that identifies the incident.')}
        value={config.key_prefix}
        onChange={handleChange}
        required
      />
      <Input
        id="pagerduty-notification-v2-client_name"
        name="client_name"
        label="Client Name"
        type="text"
        bsStyle={validation.errors.client_name ? 'error' : null}
        help={get(
          validation,
          'errors.client_name[0]',
          `The name of the ${productName} system that is triggering the PagerDuty event.`,
        )}
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
        help={get(
          validation,
          'errors.client_url[0]',
          `The URL of the ${productName} system that is triggering the PagerDuty event.`,
        )}
        value={config.client_url}
        onChange={handleChange}
        required
      />
    </>
  );
};

export default PagerDutyNotificationForm;
