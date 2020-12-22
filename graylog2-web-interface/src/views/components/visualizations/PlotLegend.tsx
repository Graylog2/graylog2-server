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
import { useContext } from 'react';

import Value from 'views/components/Value';
import { useStore } from 'stores/connect';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import ChartColorContext from 'views/components/visualizations/ChartColorContext';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import FieldType from 'views/logic/fieldtypes/FieldType';

const ColorHint = styled.div(({ color }) => `
  background: ${color};
  width: 12px;
  height: 12px;
`);

const Container = styled.div`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto min-content;
  grid-template-areas: "." ".";
  height: 100%;
`;

const Legend = styled.div`
  padding: 5px;
  max-height: 80px;
  overflow: auto;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: stretch;
  align-content: stretch;
`;

const LegendEntry = styled.div`
  margin: 4px;
  display: flex;
  margin-right: 22px;
`;
const ValueContainer = styled.div`
  margin-left: 8px; 
  line-height: 12px;
`;

type Props = {
  children: React.ReactNode,
  config: AggregationWidgetConfig,
  chartData: any,
};

const PlotLegend = ({ children, config, chartData }: Props) => {
  const { columnPivots } = config;
  const fields = columnPivots.map(({ field }) => field);
  const values = chartData.map(({ name }) => name);
  const { activeQuery } = useStore(CurrentViewStateStore);
  const { colors } = useContext(ChartColorContext);

  const result = values.map((value) => {
    let val = value;
    if (fields.length === 1) {
      val = (<Value type={FieldType.Unknown} value={value} field={fields[0]} queryId={activeQuery}>{value}</Value>);
    }

    return (
      <LegendEntry>
        <ColorHint color={colors.get(value)} />
        <ValueContainer>
          {val}
        </ValueContainer>
      </LegendEntry>
    );
  });

  return (
    <Container>
      {children}
      <Legend>{result}</Legend>
    </Container>
  );
};

export default PlotLegend;
