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

import usePluginEntities from 'hooks/usePluginEntities';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import type { Rows, Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { KeyMapper } from 'views/components/visualizations/TransformKeys';
import collectKeysByType from 'views/components/visualizations/keyMappers/collectKeysByType';

import KeyMapperContext from './KeyMapperContext';

type Props = {
  data: { [dataset: string]: Rows | unknown };
  config: AggregationWidgetConfig;
  fields: FieldTypeMappingsList;
  children: React.ReactNode;
};

const KeyMapperProvider = ({ data, config, fields, children }: Props) => {
  const bindings = usePluginEntities('visualizationKeyMappers');

  const relevantTypes = useMemo(() => new Set((bindings ?? []).map((binding) => binding.type)), [bindings]);

  const fieldTypeOf = useCallback((field: string) => fieldTypeFor(field, fields)?.type, [fields]);

  const keysByType = useMemo(
    () => collectKeysByType(data, config, fieldTypeOf, relevantTypes),
    [data, config, fieldTypeOf, relevantTypes],
  );

  // Each binding contributes a resolver hook. Calling them while iterating is safe because plugin
  // entries are registered once at module load, so this is a stable-length, stable-order list of
  // hook calls on every render (the Rules of Hooks preconditions hold).
  const mappers = (bindings ?? []).map((binding) => binding.useKeyMapper(keysByType[binding.type] ?? []));
  const mapperByType = Object.fromEntries(
    (bindings ?? []).map((binding, idx) => [binding.type, mappers[idx]] as const),
  );

  const fieldTypeByName = useMemo(() => {
    const pivotFields = [
      ...(config?.rowPivots ?? []).flatMap((pivot) => pivot.fields),
      ...(config?.columnPivots ?? []).flatMap((pivot) => pivot.fields),
    ];

    return Object.fromEntries(pivotFields.map((name) => [name, fieldTypeOf(name)]));
  }, [config, fieldTypeOf]);

  // Not memoized on purpose: the mapper must change identity whenever a binding's resolution
  // changes (e.g. async asset titles arrive) so downstream chart memos recompute. The chart data
  // itself is already recomputed every render (the rows object is rebuilt upstream per render),
  // so there is no extra cost to stabilize against.
  const mapKeys: KeyMapper = (key: Key, field: string) => {
    const type = fieldTypeByName[field];

    return (type ? mapperByType[type]?.(key) : undefined) ?? key;
  };

  return <KeyMapperContext.Provider value={mapKeys}>{children}</KeyMapperContext.Provider>;
};

export default KeyMapperProvider;
