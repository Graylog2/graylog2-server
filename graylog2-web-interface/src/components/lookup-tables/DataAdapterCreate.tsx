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
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Select } from 'components/common';
import { Row, Col, Input } from 'components/bootstrap';
import { DataAdapterForm } from 'components/lookup-tables';
import ObjectUtils from 'util/ObjectUtils';
import { useFetchDataAdapterTypes, useValidateDataAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

type DataAdapterCreateProps = {
  saved: (adapterObj: LookupTableAdapter) => void;
  onCancel: () => void;
  validationErrors?: any;
};

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

const DataAdapterCreate = ({ saved, onCancel, validationErrors = {} }: DataAdapterCreateProps) => {
  const [type, setType] = React.useState<string | undefined>(undefined);
  const [dataAdapter, setDataAdapter] = React.useState<any>(undefined);
  const { types, fetchingDataAdapterTypes } = useFetchDataAdapterTypes();
  const { validateDataAdapter } = useValidateDataAdapter();

  const adapterPlugins = React.useMemo(() => {
    const plugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      plugins[p.type] = p;
    });

    return plugins;
  }, []);

  const sortedAdapters = React.useMemo(() => {
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

  const handleTypeSelect = React.useCallback(
    (adapterType: string) => {
      const defaultConfig = ObjectUtils.clone(types[adapterType].default_config);

      setType(adapterType);
      setDataAdapter({
        id: null,
        title: '',
        name: '',
        description: '',
        config: defaultConfig,
      });
    },
    [types],
  );

  const validate = (adapter) => {
    validateDataAdapter(adapter);
  };

  return (
    <div>
      <StyledRow>
        <Col lg={6}>
          <Input
            id="data-adapter-type-select"
            label="Data Adapter Type"
            required
            autoFocus
            help="The type of data adapter to configure.">
            <Select
              placeholder="Select Data Adapter Type"
              clearable={false}
              options={sortedAdapters}
              onChange={handleTypeSelect}
              value={null}
            />
          </Input>
        </Col>
      </StyledRow>
      {dataAdapter && (
        <StyledRow>
          <Col lg={9}>
            <DataAdapterForm
              dataAdapter={dataAdapter}
              type={type}
              create
              title="Configure Adapter"
              validate={validate}
              validationErrors={validationErrors}
              saved={saved}
              onCancel={onCancel}
            />
          </Col>
        </StyledRow>
      )}
    </div>
  );
};

export default DataAdapterCreate;
