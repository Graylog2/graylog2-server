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

import AppConfig from 'util/AppConfig';
import ObjectUtils from 'util/ObjectUtils';
import { Input } from 'components/bootstrap';
import { Select, TimeUnitInput } from 'components/common';
import type { ValidationState } from 'components/common/types';

export type Config = {
  path: string;
  database_type: string;
  check_interval: number;
  check_interval_unit: string;
};
type MaxmindAdapterFieldSetProps = {
  config: Config;
  updateConfig: (newConfig: object) => void;
  handleFormEvent: (e: { target: { name: string; value?: string } }) => void;
  validationState: (key: string) => ValidationState;
  validationMessage: (key: string, message: string) => string | undefined;
};

const MaxmindAdapterFieldSet = ({
  config,
  updateConfig,
  handleFormEvent,
  validationState,
  validationMessage,
}: MaxmindAdapterFieldSetProps) => {
  const { t } = useTranslation('common');
  const isCloud = AppConfig.isCloud();

  const pathsForCloud = {
    IPINFO_STANDARD_LOCATION: '/etc/graylog/server/standard_location.mmdb',
    IPINFO_ASN: '/etc/graylog/server/asn.mmdb',
  };

  const ipInfoDatabaseTypes = [
    { label: t('maxmind_adapter.database_types.ipinfo_location'), value: 'IPINFO_STANDARD_LOCATION' },
    { label: t('maxmind_adapter.database_types.ipinfo_asn'), value: 'IPINFO_ASN' },
  ];

  let databaseTypes = [
    { label: t('maxmind_adapter.database_types.asn'), value: 'MAXMIND_ASN' },
    { label: t('maxmind_adapter.database_types.city'), value: 'MAXMIND_CITY' },
    { label: t('maxmind_adapter.database_types.country'), value: 'MAXMIND_COUNTRY' },
  ];

  if (isCloud) {
    databaseTypes = ipInfoDatabaseTypes;
  } else {
    databaseTypes = databaseTypes.concat(ipInfoDatabaseTypes);
  }

  const update = (value: number, unit: string, enabled: boolean, name: string) => {
    const newConfig = ObjectUtils.clone(config);

    newConfig[name] = enabled ? value : 0;
    newConfig[`${name}_unit`] = unit;
    updateConfig(newConfig);
  };

  const updateCheckInterval = (value: number, unit: string, enabled: boolean) => {
    update(value, unit, enabled, 'check_interval');
  };

  const onDbTypeSelect = (id: string) => {
    const newConfig = ObjectUtils.clone(config);

    if (isCloud) {
      newConfig.path = pathsForCloud[id];
    }

    newConfig.database_type = id;
    updateConfig(newConfig);
  };

  return (
    <fieldset>
      {!isCloud && (
        <Input
          type="text"
          id="path"
          name="path"
          label={t('maxmind_adapter.field_set.file_path')}
          autoFocus
          required
          onChange={handleFormEvent}
          help={validationMessage('path', t('maxmind_adapter.field_set.file_path_help'))}
          bsStyle={validationState('path')}
          value={config.path}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
      )}
      <Input
        id="database-type-select"
        label={t('maxmind_adapter.field_set.database_type')}
        required
        autoFocus
        help={t('maxmind_adapter.field_set.database_type_help')}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9">
        <Select
          placeholder={t('maxmind_adapter.field_set.database_type_placeholder')}
          clearable={false}
          options={databaseTypes}
          onChange={onDbTypeSelect}
          value={config.database_type}
        />
      </Input>
      <TimeUnitInput
        label={t('maxmind_adapter.field_set.refresh_file')}
        help={t('maxmind_adapter.field_set.refresh_file_help')}
        update={updateCheckInterval}
        value={config.check_interval}
        unit={config.check_interval_unit || 'MINUTES'}
        defaultEnabled={config.check_interval > 0}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
    </fieldset>
  );
};

export default MaxmindAdapterFieldSet;
