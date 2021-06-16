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
import { useContext, useState, useCallback } from 'react';
import styled from 'styled-components';
import { Overlay, RootCloseWrapper } from 'react-overlays';

import ColorPicker from 'components/common/ColorPicker';
import Value from 'views/components/Value';
import { useStore } from 'stores/connect';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import ChartColorContext from 'views/components/visualizations/ChartColorContext';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { Popover } from 'components/graylog';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { colors as defaultColors } from 'views/components/visualizations/Colors';
import { EVENT_COLOR, eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import Series from 'views/logic/aggregationbuilder/Series';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

const ColorHint = styled.div(({ color }) => `
  cursor: pointer;
  background-color: ${color} !important; /* Needed for report generation */
  -webkit-print-color-adjust: exact !important; /* Needed for report generation */
  width: 12px;
  height: 12px;
`);

const Container = styled.div`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 4fr auto;
  grid-template-areas: "." ".";
  height: 100%;
`;

const LegendContainer = styled.div`
  padding: 5px;
  max-height: 100px;
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
  labelMapper?: (data: Array<any>) => Array<string> | undefined | null,
  neverHide?: boolean,
};

type ColorPickerConfig = {
  name: string,
  target: EventTarget,
};

const isLabelAFunction = (label: string, series: Series) => series.function === label || series.config.name === label;

const legendField = (columnPivots: Array<Pivot>, rowPivots: Array<Pivot>, neverHide: boolean, series: Array<Series>, value: string) => {
  if (columnPivots.length === 1 && series.length === 1 && !isLabelAFunction(value, series[0])) {
    return columnPivots[0].field;
  }

  if (!neverHide && rowPivots.length === 1) {
    return rowPivots[0].field;
  }

  return null;
};

const defaultLabelMapper = (data: Array<{ name: string }>) => data.map(({ name }) => name);

const PlotLegend = ({ children, config, chartData, labelMapper = defaultLabelMapper, neverHide }: Props) => {
  const [colorPickerConfig, setColorPickerConfig] = useState<ColorPickerConfig | undefined>();
  const { rowPivots, columnPivots, series } = config;
  const labels: Array<string> = labelMapper(chartData);
  const { activeQuery } = useStore(CurrentViewStateStore);
  const { colors, setColor } = useContext(ChartColorContext);
  const { focusedWidget } = useContext(WidgetFocusContext);

  const chunkCells = (cells, columnCount) => {
    const { length } = cells;
    let rowCount;

    if (length <= columnCount) {
      rowCount = 1;
    } else {
      rowCount = Math.round(length / columnCount) + 1;
    }

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

  const _onCloseColorPicker = useCallback(() => setColorPickerConfig(undefined), [setColorPickerConfig]);

  const _onOpenColorPicker = useCallback((field) => (event) => {
    setColorPickerConfig({ name: field, target: event.currentTarget });
  }, [setColorPickerConfig]);

  const _onColorSelect = useCallback((field: string, color: string) => {
    setColor(field, color);
    setColorPickerConfig(undefined);
  }, [setColor]);

  if (!neverHide && (!focusedWidget || !focusedWidget.editing) && series.length <= 1 && columnPivots.length <= 0) {
    return <>{children}</>;
  }

  const tableCells = labels.sort(stringLenSort).map((value) => {
    const defaultColor = value === eventsDisplayName ? EVENT_COLOR : undefined;
    const field = legendField(columnPivots, rowPivots, !neverHide, series, value);
    const val = field !== null ? <Value type={FieldType.Unknown} value={value} field={field} queryId={activeQuery}>{value}</Value> : value;

    return (
      <LegendCell key={value}>
        <LegendEntry>
          <ColorHint aria-label="Color Hint" onClick={_onOpenColorPicker(value)} color={colors.get(value, defaultColor)} />
          <ValueContainer>
            {val}
          </ValueContainer>
        </LegendEntry>
      </LegendCell>
    );
  });

  const result = chunkCells(tableCells, 5).map((cells, index) => (
    // eslint-disable-next-line react/no-array-index-key
    <LegendRow key={index}>
      {cells}
    </LegendRow>
  ));

  return (
    <Container>
      {children}
      <LegendContainer>
        <Legend>{result}</Legend>
      </LegendContainer>
      {colorPickerConfig && (
        <RootCloseWrapper event="mousedown"
                          onRootClose={_onCloseColorPicker}>
          <Overlay show
                   placement="top"
                   target={colorPickerConfig.target}>
            <Popover id="legend-config"
                     title={`Configuration for ${colorPickerConfig.name}`}>
              <ColorPicker color={colors.get(colorPickerConfig.name)}
                           colors={defaultColors}
                           onChange={(newColor) => _onColorSelect(colorPickerConfig.name, newColor)} />
            </Popover>
          </Overlay>
        </RootCloseWrapper>
      )}
    </Container>
  );
};

PlotLegend.defaultProps = {
  labelMapper: defaultLabelMapper,
  neverHide: false,
};

export default PlotLegend;
