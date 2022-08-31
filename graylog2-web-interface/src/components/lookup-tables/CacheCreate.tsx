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
import type { LookupTableCache, validationErrorsType } from 'src/logic/lookup-tables/types';

import usePluginEntities from 'hooks/usePluginEntities';
import { Row, Col, Input } from 'components/bootstrap';
import { Select } from 'components/common';

import CacheForm from './CacheForm';
import type { CachePluginType } from './types';

const INIT_CACHE: LookupTableCache = {
  id: null,
  title: '',
  name: '',
  description: '',
  config: { type: 'none' },
};

type TypesType = { type?: string, lable?: string };
type OptionType = { value: string, label: string }

type Props = {
  saved: () => void,
  types: TypesType[],
  validate: () => void,
  validationErrors: validationErrorsType,
};

const CacheCreate = ({ saved, types, validate, validationErrors }: Props) => {
  const [type, setType] = React.useState<string>(null);
  const cachePlugins = usePluginEntities('lookupTableCaches');

  const plugins = React.useMemo(() => (
    cachePlugins.reduce((acc: any, plugin: CachePluginType) => {
      acc[plugin.type] = plugin;

      return acc;
    }, {})
  ), [cachePlugins]);

  const cacheTypes = React.useMemo(() => (
    Object.values(types)
      .map((inType: TypesType) => ({ value: inType.type, label: plugins[inType.type].displayName }))
      .sort((a: OptionType, b: OptionType) => {
        if (a.label.toLowerCase() > b.label.toLowerCase()) return 1;
        if (a.label.toLowerCase() < b.label.toLowerCase()) return -1;

        return 0;
      })
  ), [types, plugins]);

  const cache = React.useMemo(() => {
    if (type) {
      return {
        ...INIT_CACHE,
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
