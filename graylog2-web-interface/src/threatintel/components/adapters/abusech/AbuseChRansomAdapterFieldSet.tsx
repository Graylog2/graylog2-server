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

type AbuseChRansomAdapterFieldSetProps = {
  config: any;
  updateConfig: (...args: any[]) => void;
  // eslint-disable-next-line react/no-unused-prop-types
  handleFormEvent: (...args: any[]) => void;
  // eslint-disable-next-line react/no-unused-prop-types
  validationState: (...args: any[]) => void;
  // eslint-disable-next-line react/no-unused-prop-types
  validationMessage: (...args: any[]) => void;
} & WithTranslation;

class AbuseChRansomAdapterFieldSet extends React.Component<
  AbuseChRansomAdapterFieldSetProps,
  {
    [key: string]: any;
  }
> {
  _update = (value, unit, enabled, name) => {
    const config = ObjectUtils.clone(this.props.config);
    config[name] = enabled ? value : 0;
    config[`${name}_unit`] = unit;
    this.props.updateConfig(config);
  };

  updateRefreshInterval = (value, unit, enabled) => {
    this._update(value, unit, enabled, 'refresh_interval');
  };

  _onBlocklistTypeSelect = (id) => {
    const config = ObjectUtils.clone(this.props.config);
    config.blocklist_type = id;
    this.props.updateConfig(config);
  };

  render() {
    const { config, t } = this.props;
    const blocklistTypes = [
      { label: t('abusech_ransom_adapter_field_set.domains_blocklist'), value: 'DOMAINS' },
      { label: t('abusech_ransom_adapter_field_set.urls_blocklist'), value: 'URLS' },
      { label: t('abusech_ransom_adapter_field_set.ips_blocklist'), value: 'IPS' },
    ];

    return (
      <fieldset>
        <Input
          label={t('abusech_ransom_adapter_field_set.blocklist_type_label')}
          id="blocklist-type-selector"
          required
          autoFocus
          help={t('abusech_ransom_adapter_field_set.blocklist_type_help')}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9">
          <Select
            placeholder={t('abusech_ransom_adapter_field_set.blocklist_type_placeholder')}
            clearable={false}
            options={blocklistTypes}
            onChange={this._onBlocklistTypeSelect}
            value={config.blocklist_type}
          />
        </Input>
        <TimeUnitInput
          label={t('abusech_ransom_adapter_field_set.refresh_blocklist_label')}
          help={t('abusech_ransom_adapter_field_set.refresh_blocklist_help')}
          update={this.updateRefreshInterval}
          value={config.refresh_interval}
          unit={config.refresh_interval_unit || 'MINUTES'}
          defaultEnabled={config.refresh_interval > 0}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
      </fieldset>
    );
  }
}

export default withTranslation('integrations')(AbuseChRansomAdapterFieldSet);
