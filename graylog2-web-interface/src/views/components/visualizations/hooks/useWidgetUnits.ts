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
import { useMemo } from 'react';
import compact from 'lodash/compact';
import uniq from 'lodash/uniq';

import useFieldTypesUnits from 'views/hooks/useFieldTypesUnits';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type Series from 'views/logic/aggregationbuilder/Series';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';

const useWidgetUnits = (config: AggregationWidgetConfig) => {
  const fieldTypesUnits = useFieldTypesUnits();
  const usedFieldsInSeries = useMemo(() => config.series.map((s: Series) => {
    const { field } = parseSeries(s.function) ?? {};

    return field;
  }), [config.series]);
  const usedFieldsInRowPivots = useMemo(() => config.rowPivots.flatMap((p) => p.fields), [config.rowPivots]);
  const usedFieldsInColumnPivots = useMemo(() => config.columnPivots.flatMap((p) => p.fields), [config.columnPivots]);

  const allFields: Array<string> = useMemo(() => uniq(compact([...usedFieldsInSeries, ...usedFieldsInRowPivots, ...usedFieldsInColumnPivots])), [usedFieldsInColumnPivots, usedFieldsInRowPivots, usedFieldsInSeries]);

  return useMemo(() => {
    const widgetUnits = config.units;

    const filtratedFields = Object.fromEntries(allFields.filter((field) => {
      const predefinedUnit = fieldTypesUnits?.[field];

      return !widgetUnits?.getFieldUnit(field) && !!predefinedUnit;
    }).map((field) => {
      const predefinedUnit = fieldTypesUnits?.[field];

      return [field, predefinedUnit];
    }));

    return widgetUnits.toBuilder().merge(filtratedFields).build();
  }, [allFields, config?.units, fieldTypesUnits]);
};

export default useWidgetUnits;
