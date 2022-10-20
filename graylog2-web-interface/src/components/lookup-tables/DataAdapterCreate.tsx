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

import type {
  LookupTableAdapter,
  validationErrorsType,
  LUTTypesAPIResponse,
  LUTTypesType,
} from 'logic/lookup-tables/types';
import usePluginEntities from 'hooks/usePluginEntities';
import { Select } from 'components/common';
import { Row, Col, Input } from 'components/bootstrap';
import { DataAdapterForm } from 'components/lookup-tables';

import type { DataAdapterPluginType } from './types';

const INIT_DATA_ADAPTER: LookupTableAdapter = {
  id: null,
  title: '',
  name: '',
  description: '',
  config: { type: 'none' },

};

type OptionType = { value: string, label: string, disabled?: boolean };

type Props = {
  saved: () => void,
  types: LUTTypesAPIResponse,
  validate: () => void,
  validationErrors: validationErrorsType,
};

const DataAdapterCreate = ({ saved, types, validate, validationErrors }: Props) => {
  const [type, setType] = React.useState<string>(null);
  const dataAdapterPlugins = usePluginEntities('lookupTableAdapters');

  const plugins = React.useMemo(() => (
    dataAdapterPlugins.reduce((acc: any, plugin: DataAdapterPluginType) => {
      acc[plugin.type] = plugin;

      return acc;
    }, {})
  ), [dataAdapterPlugins]);

  const adapterTypes = React.useMemo(() => (
    Object.values(types)
      .map((inType: LUTTypesType) => {
        if (!plugins[inType.type]) {
          // eslint-disable-next-line no-console
          console.error(`${inType.type} - missing or invalid plugin`);

          return {
            value: inType.type,
            disabled: true,
            label: `${inType.type} - missing or invalid plugin`,
          };
        }

        return { value: inType.type, label: plugins[inType.type].displayName };
      }).sort((a: OptionType, b: OptionType) => {
        if (a.label.toLowerCase() > b.label.toLowerCase()) return 1;
        if (a.label.toLowerCase() < b.label.toLowerCase()) return -1;

        return 0;
      })
  ), [types, plugins]);

  const dataAdapter = React.useMemo(() => {
    if (type) {
      return {
        ...INIT_DATA_ADAPTER,
        config: { ...types[type]?.default_config },
      };
    }

    return null;
  }, [type, types]);

  const handleSelect = (selectedType: string) => {
    setType(selectedType);
  };

  return (
    <>
      <Row className="content">
        <Col lg={8}>
          <form className="form form-horizontal" onSubmit={() => {}}>
            <Input id="data-adapter-type-select"
                   label="Data Adapter Type"
                   required
                   autoFocus
                   help="The type of data adapter to configure."
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select placeholder="Select Data Adapter Type"
                      clearable={false}
                      options={adapterTypes}
                      matchProp="label"
                      onChange={handleSelect}
                      value={null} />
            </Input>
          </form>
        </Col>
      </Row>
      {dataAdapter && (
        <Row className="content">
          <Col lg={12}>
            <DataAdapterForm dataAdapter={dataAdapter}
                             type={type}
                             create
                             title="Configure Adapter"
                             validate={validate}
                             validationErrors={validationErrors}
                             saved={saved} />
          </Col>
        </Row>
      )}
    </>
  );
};

export default DataAdapterCreate;
