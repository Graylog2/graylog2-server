import React from 'react';

import AppConfig from 'util/AppConfig';
import ObjectUtils from 'util/ObjectUtils';
import { Input } from 'components/bootstrap';
import { Select, TimeUnitInput } from 'components/common';

export type Config = {
  path: string,
  database_type: string,
  check_interval: number,
  check_interval_unit: string,
};
type MaxmindAdapterFieldSetProps = {
  config: Config;
  updateConfig: (newConfig: object) => void,
  handleFormEvent: (e: { target: { name: string; value?: string } }) => void;
  validationState: (key: string) => string | undefined,
  validationMessage: (key: string, message: string) => string | undefined,
};

const MaxmindAdapterFieldSet = ({ config, updateConfig, handleFormEvent, validationState, validationMessage }: MaxmindAdapterFieldSetProps) => {
  const isCloud = AppConfig.isCloud();

  const pathsForCloud = {
    IPINFO_STANDARD_LOCATION: '/etc/graylog/server/standard_location.mmdb',
    IPINFO_ASN: '/etc/graylog/server/asn.mmdb',
  };

  const ipInfoDatabaseTypes = [
    { label: 'IPinfo location database', value: 'IPINFO_STANDARD_LOCATION' },
    { label: 'IPinfo ASN database', value: 'IPINFO_ASN' },
  ];

  let databaseTypes = [
    { label: 'ASN database', value: 'MAXMIND_ASN' },
    { label: 'City database', value: 'MAXMIND_CITY' },
    { label: 'Country database', value: 'MAXMIND_COUNTRY' },
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
      <Input type="text"
             id="path"
             name="path"
             label="File path"
             autoFocus
             required
             onChange={handleFormEvent}
             help={validationMessage('path', 'The path to the database file.')}
             bsStyle={validationState('path')}
             value={config.path}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      )}
      <Input id="database-type-select"
             label="Database type"
             required
             autoFocus
             help="Select the type of the database file"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9">
        <Select placeholder="Select the type of database file"
                clearable={false}
                options={databaseTypes}
                matchProp="label"
                onChange={onDbTypeSelect}
                value={config.database_type} />
      </Input>
      <TimeUnitInput label="Refresh file"
                     help="If enabled, the database file is checked for modifications and refreshed when it changed on disk."
                     update={updateCheckInterval}
                     value={config.check_interval}
                     unit={config.check_interval_unit || 'MINUTES'}
                     defaultEnabled={config.check_interval > 0}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
    </fieldset>
  );
};

export default MaxmindAdapterFieldSet;
