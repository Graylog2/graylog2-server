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
import { useContext } from 'react';
import styled from 'styled-components';

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

const LegendContainer = styled.div`
  padding: 5px;
  max-height: 80px;
  overflow: auto;
`;

const Legend = styled.div`
  display: table;
  width: 100%;
`;

const LegendRow = styled.div`
  display: table-row;
`;

const LegendCell = styled.div`
  padding: 4px;
  display: table-cell;
`;

const LegendEntry = styled.div`
  display: flex;
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
  const values: Array<string> = chartData.map(({ name }) => name);
  const { activeQuery } = useStore(CurrentViewStateStore);
  const { colors } = useContext(ChartColorContext);

  const chunkCells = (cells, columnCount) => {
    const { length } = cells;
    const rowCount = Math.round(length / columnCount) + 1;
    const result = new Array(rowCount);

    for (let row = 0; row < rowCount; row += 1) {
      result[row] = [];

      for (let column = 0; column < columnCount; column += 1) {
        if (cells[(rowCount * column) + row]) {
          result[row][column] = cells[(rowCount * column) + row];
        }
      }
    }

    return result;
  };

  const stringLenSort = (s1: string, s2: string) => {
    if (s1.length < s2.length) {
      return -1;
    }

    if (s1.length === s2.length) {
      return 0;
    }

    return 1;
  };

  const tableCells = values.sort(stringLenSort).map((value) => {
    let val: React.ReactNode = value;

    if (fields.length === 1) {
      val = (<Value type={FieldType.Unknown} value={value} field={fields[0]} queryId={activeQuery}>{value}</Value>);
    }

    return (
      <LegendCell key={value}>
        <LegendEntry>
          <ColorHint color={colors.get(value)} />
          <ValueContainer>
            {val}
          </ValueContainer>
        </LegendEntry>
      </LegendCell>
    );
  });

  const result = chunkCells(tableCells, 5).map((cells) => (
    <LegendRow>
      {cells}
    </LegendRow>
  ));

  return (
    <Container>
      {children}
      <LegendContainer>
        <Legend>{result}</Legend>
      </LegendContainer>
    </Container>
  );
};

export default PlotLegend;
