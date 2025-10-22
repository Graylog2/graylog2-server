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
import type { Shape, Annotations } from 'plotly.js';
import type { DefaultTheme } from 'styled-components';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import { convertValueToBaseUnit, getPrettifiedValue } from 'views/components/visualizations/utils/unitConverters';
import type UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import type { MappersForYAxis } from 'views/components/visualizations/utils/chartLayoutGenerators';
import { getYAxisPosition } from 'views/components/visualizations/utils/chartLayoutGenerators';
import { NO_FIELD_NAME_SERIES } from 'views/components/visualizations/Constants';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';

type Props = {
  series: AggregationWidgetConfig['series'];
  widgetUnits: UnitsConfig;
  fieldNameToAxisNameMapper: MappersForYAxis['fieldNameToAxisNameMapper'];
  mapperAxisNumber: MappersForYAxis['mapperAxisNumber'];
  theme: DefaultTheme;
};

type Result = {
  shapes: Array<Partial<Shape>>;
  annotations: Array<Partial<Annotations>>;
};

const getThresholdShapes = ({
  series,
  widgetUnits,
  fieldNameToAxisNameMapper,
  mapperAxisNumber,
  theme,
}: Props): Result => {
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

      const seriesName = curSeries.config.name || curSeries.function;
      const axisStartPosition = getYAxisPosition(mapperAxisNumber[seriesName] ?? 1);

      const shape: Partial<Shape> = {
        type: 'line',
        x0: axisStartPosition,
        x1: 1,
        y0: baseUnitValue,
        y1: baseUnitValue,
        xref: 'paper',
        yref,
        name,
        line: {
          color,
        },
      };

      const bgColor = theme.utils.opacify(color, 0.6);
      const contrastColor = theme.utils.readableColor(bgColor);
      const isLeft = axisStartPosition < 0.5;

      const annotation: Partial<Annotations> = {
        x: axisStartPosition,
        y: baseUnitValue,
        xref: 'paper',
        yref,
        text: `${name} (${formattedValueWithUnitLabel})`,
        showarrow: false,
        xanchor: isLeft ? 'left' : 'right',
        yanchor: 'bottom',
        align: isLeft ? 'left' : 'right',
        font: { color: contrastColor, size: 12 },
        xshift: 0,
        yshift: 0,
        bgcolor: bgColor,
        bordercolor: bgColor,
        borderpad: 3,
      };

      return { shape, annotation };
    });
  });

  const res = flatten(thresholds).filter((threshold) => !!threshold?.shape);

  return {
    shapes: res.map(({ shape }) => shape),
    annotations: res.map(({ annotation }) => annotation),
  };
};

export default getThresholdShapes;
