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
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import ObjectUtils from 'util/ObjectUtils';
import { Input } from 'components/bootstrap';
import { Select, TimeUnitInput } from 'components/common';
import type { ValidationState } from 'components/common/types';

type DnsAdapterFieldSetProps = {
  config: {
    request_timeout: number;
    server_ips?: string;
    lookup_type?: string;
    cache_ttl_override: string;
    cache_ttl_override_unit: string;
    cache_ttl_override_enabled: boolean;
  };
  updateConfig: (...args: any[]) => void;
  handleFormEvent: (...args: any[]) => void;
  validationMessage: (...args: any[]) => string;
  validationState: (...args: any[]) => ValidationState;
} & WithTranslation;

class DnsAdapterFieldSet extends React.Component<
  DnsAdapterFieldSetProps,
  {
    [key: string]: any;
  }
> {
  _onLookupTypeSelect = (id) => {
    const { config, updateConfig } = this.props;
    const newConfig = ObjectUtils.clone(config);

    newConfig.lookup_type = id;
    updateConfig(newConfig);
  };

  updateCacheTTLOverride = (value, unit, enabled) => {
    this._updateCacheTTLOverride(value, unit, enabled, 'cache_ttl_override');
  };

  _updateCacheTTLOverride = (value, unit, enabled, fieldPrefix) => {
    const { config, updateConfig } = this.props;
    const newConfig = ObjectUtils.clone(config);

    // If Cache TTL Override box is checked, then save the value. If not, then do not save it.
    if (enabled && value) {
      newConfig[fieldPrefix] = enabled && value ? value : null;
      newConfig[`${fieldPrefix}_enabled`] = enabled;
    } else {
      newConfig[fieldPrefix] = null;
      newConfig[`${fieldPrefix}_enabled`] = false;
    }

    newConfig[`${fieldPrefix}_unit`] = enabled ? unit : null;
    updateConfig(newConfig);
  };

  render() {
    const { config, handleFormEvent, validationMessage, validationState, t } = this.props;
    const lookupTypes = [
      { label: t('dns_adapter.lookup_type_a'), value: 'A' },
      { label: t('dns_adapter.lookup_type_aaaa'), value: 'AAAA' },
      { label: t('dns_adapter.lookup_type_a_aaaa'), value: 'A_AAAA' },
      { label: t('dns_adapter.lookup_type_ptr'), value: 'PTR' },
      { label: t('dns_adapter.lookup_type_txt'), value: 'TXT' },
    ];

    return (
      <fieldset>
        <Input
          label={t('dns_adapter.lookup_type_label')}
          id="lookup-type"
          required
          autoFocus
          help={t('dns_adapter.lookup_type_help')}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9">
          <Select
            placeholder={t('dns_adapter.lookup_type_placeholder')}
            clearable={false}
            options={lookupTypes}
            onChange={this._onLookupTypeSelect}
            value={config.lookup_type}
          />
        </Input>
        <Input
          type="text"
          id="server_ips"
          name="server_ips"
          label={t('dns_adapter.server_ips_label')}
          onChange={handleFormEvent}
          help={validationMessage('server_ips', t('dns_adapter.server_ips_help'))}
          bsStyle={validationState('server_ips')}
          value={config.server_ips}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
        <Input
          type="number"
          id="request_timeout"
          name="request_timeout"
          label={t('dns_adapter.request_timeout_label')}
          required
          onChange={handleFormEvent}
          help={validationMessage('request_timeout', t('dns_adapter.request_timeout_help'))}
          bsStyle={validationState('request_timeout')}
          value={config.request_timeout}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
        <TimeUnitInput
          label={t('dns_adapter.cache_ttl_override')}
          help={t('dns_adapter.cache_ttl_override_help')}
          update={this.updateCacheTTLOverride}
          value={config.cache_ttl_override}
          unit={config.cache_ttl_override_unit || 'MINUTES'}
          units={['MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']}
          enabled={config.cache_ttl_override_enabled}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
      </fieldset>
    );
  }
}

export default withTranslation('lookup_tables')(DnsAdapterFieldSet);
