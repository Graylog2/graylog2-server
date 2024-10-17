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
import React, { useContext, useEffect, useRef } from 'react';
import styled, { css } from 'styled-components';

import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import Value from 'views/components/Value';
import DecoratedValue from 'views/components/messagelist/decoration/DecoratedValue';
import CustomHighlighting from 'views/components/highlighting/CustomHighlighting';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import ElementDimensions from 'components/common/ElementDimensions';

import Trend from './Trend';
import AutoFontSizer from './AutoFontSizer';

const Container = styled.div<{ $height: number }>(({ $height }) => css`
  height: ${$height}px;
  width: 100%;
`);

const GridContainer = styled(Container)`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr auto;
  grid-gap: 0;
`;

const SingleItemGrid = styled(Container)`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  grid-gap: 0;
`;

const NumberBox = styled(ElementDimensions)`
  height: 100%;
  width: 100%;
  padding-bottom: 10px;
`;

const TrendBox = styled(ElementDimensions)`
  height: 100%;
  width: 100%;
`;

const _extractValueAndField = (rows: Rows) => {
  if (!rows || !rows[0]) {
    return { value: undefined, field: undefined };
  }

  const results = rows[0];

  if (results.source === 'leaf') {
    const leaf = results.values.find((f) => f.source === 'row-leaf');

    if (leaf && leaf.source === 'row-leaf') {
      return { value: leaf.value, field: leaf.key[0] };
    }
  }

  return { value: undefined, field: undefined };
};

const _extractFirstSeriesName = (config) => {
  const { series = [] } = config;

  return series.length === 0
    ? undefined
    : series[0].function;
};

const NumberVisualization = ({ config, fields, data, height: heightProp }: VisualizationComponentProps) => {
  const targetRef = useRef();
  const onRenderComplete = useContext(RenderCompletionCallback);
  const visualizationConfig = (config.visualizationConfig as NumberVisualizationConfig) ?? NumberVisualizationConfig.create();

  const field = _extractFirstSeriesName(config);

  useEffect(onRenderComplete, [onRenderComplete]);
  const chartRows = retrieveChartData(data);
  const trendRows = data.trend;
  const { value } = _extractValueAndField(chartRows);
  const { value: previousValue } = _extractValueAndField(trendRows || []);

  if (!field || (value !== 0 && !value)) {
    return <>N/A</>;
  }

  const ContainerComponent = visualizationConfig.trend ? GridContainer : SingleItemGrid;

  return (
    <ContainerComponent $height={heightProp}>
      <NumberBox resizeDelay={20}>
        {({ height, width }) => (
          <AutoFontSizer height={height} width={width} center>
            <CustomHighlighting field={field} value={value}>
              <Value field={field}
                     type={fieldTypeFor(field, fields)}
                     value={value}
                     render={DecoratedValue} />
            </CustomHighlighting>
          </AutoFontSizer>
        )}
      </NumberBox>
      {visualizationConfig.trend && (
        <TrendBox>
          {({ height, width }) => (
            <AutoFontSizer height={height} width={width} target={targetRef}>
              <Trend ref={targetRef}
                     current={value}
                     previous={previousValue}
                     trendPreference={visualizationConfig.trendPreference} />
            </AutoFontSizer>
          )}
        </TrendBox>
      )}
    </ContainerComponent>
  );
};

const ConnectedNumberVisualization = makeVisualization(NumberVisualization, 'numeric');

export default ConnectedNumberVisualization;
