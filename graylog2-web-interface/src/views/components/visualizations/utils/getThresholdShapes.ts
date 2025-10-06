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
import flatten from 'lodash/flatten';
import type { Shape } from 'plotly.js';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import { convertValueToBaseUnit, getPrettifiedValue } from 'views/components/visualizations/utils/unitConverters';
import type UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import type { MappersForYAxis } from 'views/components/visualizations/utils/chartLayoutGenerators';
import { NO_FIELD_NAME_SERIES } from 'views/components/visualizations/Constants';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';

const getThresholdShapes = (
  series: AggregationWidgetConfig['series'],
  widgetUnits: UnitsConfig,
  fieldNameToAxisNameMapper: MappersForYAxis['fieldNameToAxisNameMapper'],
): Array<Partial<Shape>> => {
  const thresholds = series?.map((curSeries) => {
    const { field } = parseSeries(curSeries.function) ?? {};
    const seriesUnit = widgetUnits.getFieldUnit(field);

    return curSeries.config.thresholds?.map(({ color, value, name }) => {
      const conversionParams =
        seriesUnit && seriesUnit.isDefined ? { abbrev: seriesUnit?.abbrev, unitType: seriesUnit?.unitType } : null;

      const baseUnitValue = conversionParams ? convertValueToBaseUnit(Number(value), conversionParams).value : value;
      const yref: Shape['yref'] = fieldNameToAxisNameMapper?.[field ?? NO_FIELD_NAME_SERIES] ?? 'y';

      const prettified = conversionParams ? getPrettifiedValue(Number(value), conversionParams) : null;
      const formattedValueWithUnitLabel = prettified
        ? formatValueWithUnitLabel(prettified?.value, prettified.unit.abbrev, 0)
        : value;

      const shape: Partial<Shape> = {
        type: 'line',
        x0: 0,
        x1: 1,
        y0: baseUnitValue,
        y1: baseUnitValue,
        xref: 'paper',
        yref,
        name,
        line: {
          color,
        },
        label: {
          text: `${name} (${formattedValueWithUnitLabel})`,
          textposition: 'top right',
          font: {
            color,
          },
        },
      };

      return shape;
    });
  });

  return flatten(thresholds).filter((th) => !!th);
};

export default getThresholdShapes;
