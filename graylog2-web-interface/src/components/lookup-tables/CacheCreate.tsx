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

import usePluginEntities from 'hooks/usePluginEntities';
import { Row, Col, Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { useFetchCacheTypes, useValidateCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import CacheForm from 'components/lookup-tables/CacheForm';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import type { CachePluginType } from './types';

const INIT_CACHE: LookupTableCache = {
  id: null,
  title: '',
  name: '',
  description: '',
  config: { type: 'none' },
};

type TypesType = { type?: string; lable?: string };
type cacheTypeOptionsType = { value: string; label: string };

type Props = {
  saved: (cacheObj: LookupTableCache) => void;
  onCancel: () => void;
  validationErrors?: any;
};

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

const CacheCreate = ({ saved, onCancel, validationErrors = {} }: Props) => {
  const [type, setType] = React.useState<string>(null);
  const cachePlugins = usePluginEntities('lookupTableCaches');
  const { types: cacheTypesFromAPI, fetchingCacheTypes } = useFetchCacheTypes();
  const { validateCache } = useValidateCache();

  const plugins = React.useMemo(
    () =>
      cachePlugins.reduce((acc: any, plugin: CachePluginType) => {
        acc[plugin.type] = plugin;

        return acc;
      }, {}),
    [cachePlugins],
  );

  const cacheTypes = React.useMemo(() => {
    if (!fetchingCacheTypes) {
      return Object.values(cacheTypesFromAPI)
        .map((inType: TypesType) => ({ value: inType.type, label: plugins[inType.type].displayName }))
        .sort((a: cacheTypeOptionsType, b: cacheTypeOptionsType) =>
          naturalSort(a.label.toLowerCase(), b.label.toLowerCase()),
        );
    }

    return [];
  }, [cacheTypesFromAPI, fetchingCacheTypes, plugins]);

  const cache = React.useMemo(() => {
    if (type) {
      return {
        ...INIT_CACHE,
        config: { ...cacheTypesFromAPI[type]?.default_config },
      };
    }

    return null;
  }, [type, cacheTypesFromAPI]);

  const handleSelect = (selectedType: string) => {
    setType(selectedType);
  };

  const validate = (cacheObj: LookupTableCache) => {
    validateCache(cacheObj);
  };

  return (
    <>
      <StyledRow>
        <Col lg={6}>
          <Input id="cache-type-select" label="Cache Type" required autoFocus help="The type of cache to configure.">
            <Select
              placeholder="Select Cache Type"
              clearable={false}
              options={cacheTypes}
              onChange={handleSelect}
              value={type}
            />
          </Input>
        </Col>
      </StyledRow>
      {cache && (
        <StyledRow>
          <Col lg={9}>
            <CacheForm
              cache={cache}
              type={type}
              title="Configure Cache"
              create
              saved={saved}
              onCancel={onCancel}
              validate={validate}
              validationErrors={validationErrors}
            />
          </Col>
        </StyledRow>
      )}
    </>
  );
};

export default CacheCreate;
