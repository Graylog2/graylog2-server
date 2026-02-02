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
import * as React from 'react';
import { useCallback, useMemo } from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import usePluginEntities from 'hooks/usePluginEntities';
import { Select } from 'components/common';
import { Input } from 'components/bootstrap';
import { useFetchDataAdapterTypes } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

type Props = {
  adapterConfigType: string;
  onAdapterChange: (adapter: LookupTableAdapter) => void;
};

function AdapterTypeSelect({ adapterConfigType, onAdapterChange }: Props) {
  const { types, fetchingDataAdapterTypes } = useFetchDataAdapterTypes();
  const plugins = usePluginEntities('lookupTableAdapters');
  const adapterPlugins = Object.fromEntries(plugins.map((p) => [p.type, p]));

  const sortedAdapters = useMemo(() => {
    if (!fetchingDataAdapterTypes) {
      return Object.keys(types)
        .map((key) => {
          const typeItem = types[key];

          if (!adapterPlugins[typeItem.type]) {
            // eslint-disable-next-line no-console
            console.error(
              `Plugin component for data adapter type ${typeItem.type} is missing - invalid or missing plugin?`,
            );

            return {
              value: typeItem.type,
              disabled: true,
              label: `${typeItem.type} - missing or invalid plugin`,
            };
          }

          return {
            value: typeItem.type,
            label: adapterPlugins[typeItem.type].displayName,
          };
        })
        .sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));
    }

    return [];
  }, [types, fetchingDataAdapterTypes, adapterPlugins]);

  const _getCorrectUserpasswd = (config: LookupTableAdapter['config']) => {
    if (config.user_passwd.is_set) return { is_set: true, keep_value: true };

    return { set_value: '' };
  };

  const handleTypeSelect = useCallback(
    (adapterType: string) => {
      const defaultConfig = { ...types[adapterType].default_config };
      const isLDAP = defaultConfig.type === 'LDAP';

      const configWithPassword = {
        ...defaultConfig,
        ...(isLDAP ? { user_passwd: _getCorrectUserpasswd(defaultConfig) } : {}),
      };

      onAdapterChange({
        id: null,
        title: '',
        name: '',
        description: '',
        config: configWithPassword,
      });
    },
    [onAdapterChange, types],
  );

  return (
    <Input
      id="data-adapter-type-select"
      label="Data Adapter Type"
      required
      help="The type of data adapter to configure.">
      <Select
        placeholder="Select Data Adapter Type"
        clearable={false}
        options={sortedAdapters}
        onChange={handleTypeSelect}
        value={adapterConfigType}
      />
    </Input>
  );
}

export default AdapterTypeSelect;
