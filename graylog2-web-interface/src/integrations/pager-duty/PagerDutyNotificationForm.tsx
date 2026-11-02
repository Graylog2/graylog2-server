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
  const { t } = useTranslation('integrations');
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
        label={t('pager_duty_notification_form.routing_key_label')}
        type="text"
        bsStyle={validation.errors.routing_key ? 'error' : null}
        help={validation.errors?.routing_key?.[0] ?? t('pager_duty_notification_form.routing_key_help')}
        value={config.routing_key}
        onChange={handleChange}
        required
      />
      <Input
        id="pagerduty-notification-v2-pager_duty_title"
        name="pager_duty_title"
        label={t('pager_duty_notification_form.incident_title_label')}
        type="text"
        bsStyle={validation.errors.pager_duty_title ? 'error' : null}
        help={
          validation.errors?.pager_duty_title?.[0] ??
          t('pager_duty_notification_form.incident_title_help', { productName })
        }
        value={config.pager_duty_title}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-custom_incident"
        name="custom_incident"
        label={t('pager_duty_notification_form.custom_incident_label')}
        type="checkbox"
        bsStyle={validation.errors.custom_incident ? 'error' : null}
        help={validation.errors?.custom_incident?.[0] ?? t('pager_duty_notification_form.custom_incident_help')}
        checked={config.custom_incident}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-key_prefix"
        name="key_prefix"
        label={t('pager_duty_notification_form.key_prefix_label')}
        type="text"
        bsStyle={validation.errors.key_prefix ? 'error' : null}
        help={validation.errors?.key_prefix?.[0] ?? t('pager_duty_notification_form.key_prefix_help')}
        value={config.key_prefix}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-incident_key"
        name="incident_key"
        label={t('pager_duty_notification_form.incident_key_label')}
        type="text"
        bsStyle={validation.errors.incident_key ? 'error' : null}
        help={validation.errors?.incident_key?.[0] ?? t('pager_duty_notification_form.incident_key_help')}
        value={config.incident_key}
        onChange={handleChange}
      />
      <Input
        id="pagerduty-notification-v2-client_name"
        name="client_name"
        label={t('pager_duty_notification_form.client_name_label')}
        type="text"
        bsStyle={validation.errors.client_name ? 'error' : null}
        help={
          validation.errors?.client_name?.[0] ?? t('pager_duty_notification_form.client_name_help', { productName })
        }
        value={config.client_name}
        onChange={handleChange}
        required
      />
      <Input
        id="pagerduty-notification-v2-client_url"
        name="client_url"
        label={t('pager_duty_notification_form.client_url_label')}
        type="text"
        bsStyle={validation.errors.client_url ? 'error' : null}
        help={validation.errors?.client_url?.[0] ?? t('pager_duty_notification_form.client_url_help', { productName })}
        value={config.client_url}
        onChange={handleChange}
        required
      />
    </>
  );
};

export default PagerDutyNotificationForm;
