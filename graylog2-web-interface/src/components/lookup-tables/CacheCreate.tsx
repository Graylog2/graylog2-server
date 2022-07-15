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
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { LookupTableCache } from 'src/logic/lookup-tables/types';

import { Row, Col, Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';

import CacheForm from './CacheForm';

const INIT_CACHE: LookupTableCache = {
  id: null,
  title: '',
  name: '',
  description: '',
  config: { type: 'none' },
};

type Props = {
  saved: () => void,
  types: any,
  validate: () => void,
  validationErrors: any,
};

const CacheCreate = ({ saved, types, validate, validationErrors }: Props) => {
  const [type, setType] = React.useState<string>(null);

  const plugins = React.useMemo(() => (
    PluginStore.exports('lookupTableCaches').reduce((acc: any, plugin: any) => {
      acc[plugin.type] = plugin;

      return acc;
    }, {})
  ), []);

  const cacheTypes = React.useMemo(() => (
    Object.values(types)
      .map((inType: any) => ({ value: inType.type, label: plugins[inType.type].displayName }))
      .sort((a: any, b: any) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()))
  ), [types, plugins]);

  const cache = React.useMemo(() => {
    if (type) {
      return {
        ...INIT_CACHE,
        config: { ...types[type]?.defaultConfig },
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
        <Col lg={6} className="form form-horizontal">
          <Input id="cache-type-select"
                 label="Cache Type"
                 required
                 autoFocus
                 help="The type of cache to configure."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select placeholder="Select Cache Type"
                    clearable={false}
                    options={cacheTypes}
                    matchProp="label"
                    onChange={handleSelect}
                    value={type} />
          </Input>
        </Col>
      </Row>
      {cache && (
        <Row className="content">
          <Col lg={12}>
            <CacheForm cache={cache}
                       type={type}
                       title="Configure Cache"
                       create
                       saved={saved}
                       validationErrors={validationErrors}
                       validate={validate} />
          </Col>
        </Row>
      )}
    </>
  );
};

export default CacheCreate;
