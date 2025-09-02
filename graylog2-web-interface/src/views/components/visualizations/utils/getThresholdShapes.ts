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

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import { convertValueToBaseUnit } from 'views/components/visualizations/utils/unitConverters';
import type UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import type { MappersForYAxis } from 'views/components/visualizations/utils/chartLayoutGenerators';
import { NO_FIELD_NAME_SERIES } from 'views/components/visualizations/Constants';

export type ThresholdShape = {
  type: string;
  x0: number;
  x1: number;
  y0: number;
  y1: number;
  name: string;
  xref: string;
  yref: string;
  line: {
    color: string;
  };
  label: {
    text: string;
    font: {
      color: string;
    };
  };
};

const getThresholdShapes = (
  series: AggregationWidgetConfig['series'],
  widgetUnits: UnitsConfig,
  fieldNameToAxisNameMapper: MappersForYAxis['fieldNameToAxisNameMapper'],
): Array<ThresholdShape> => {
  const thresholds = series?.map((serieso) => {
    const updatedSeriesConfig = SeriesConfig.empty()
      .toBuilder()
      .thresholds([{ value: 200, name: 'My TH', color: 'green' }])
      .build();
    const curSeries = serieso.toBuilder().config(updatedSeriesConfig).build();

    const { field } = parseSeries(curSeries.function) ?? {};
    const seriesUnit = widgetUnits.getFieldUnit(field);

    return curSeries.config.thresholds?.map(({ color, value, name }) => {
      const baseUnitValue =
        seriesUnit && seriesUnit?.isDefined
          ? convertValueToBaseUnit(value, { abbrev: seriesUnit.abbrev, unitType: seriesUnit.unitType }).value
          : value;
      const yref: string = fieldNameToAxisNameMapper?.[field ?? NO_FIELD_NAME_SERIES] ?? 'y';

      return {
        type: 'line',
        x0: 0,
        x1: 1,
        y0: baseUnitValue,
        y1: baseUnitValue,
        name: 'Traffic Limit',
        xref: 'paper',
        yref,
        line: {
          color,
        },
        label: {
          text: name,
          textposition: 'top right',
          font: {
            color,
          },
        },
      };
    });
  });

  return flatten(thresholds);
};

export default getThresholdShapes;
