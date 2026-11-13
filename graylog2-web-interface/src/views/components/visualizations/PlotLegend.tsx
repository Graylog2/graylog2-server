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
import styled, { css } from 'styled-components';

import ChartLegend from 'components/common/ChartLegend';
import type { ChartLegendItem } from 'components/common/ChartLegend';
import Value from 'views/components/Value';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import ChartColorContext from 'views/components/visualizations/ChartColorContext';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { colors as defaultColors } from 'views/components/visualizations/Colors';
import { EVENT_COLOR, eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import { keySeparator, humanSeparator } from 'views/Constants';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import { useIsInteractiveMode } from 'views/components/contexts/InteractiveContext';
import WidgetRenderingContext from 'views/components/widgets/WidgetRenderingContext';

const FixedContainer = styled.div<{ $height: number; $width: number }>`
  display: grid;
  grid-template: 4fr auto / 1fr;
  grid-template-areas: '.' '.';
  height: 100%;
`;

const VariableContainer = styled.div<{ $height: number; $width: number }>(
  ({ $height, $width }) => css`
    display: grid;
    width: ${$width}px;
    grid-template-columns: ${$width}px;
    grid-template-rows: ${$height}px auto;
    gap: 0;
    justify-content: center;
  `,
);

type Props = {
  children: React.ReactNode;
  config: AggregationWidgetConfig;
  chartData: any;
  labelFields?: (config: Props['config']) => Array<string>;
  labelMapper?: (data: Array<any>) => Array<string> | undefined | null;
  neverHide?: boolean;
  height: number;
  width: number;
};

const defaultLabelMapper = (data: Array<Pick<ChartDefinition, 'name' | 'originalName'>>) =>
  data.map(({ name, originalName }) => originalName ?? name);

const stringLenSort = (s1: string, s2: string) => {
  if (s1.length < s2.length) {
    return -1;
  }

  if (s1.length === s2.length) {
    return 0;
  }

  return 1;
};

const columnPivotsToFields = (config: Props['config']) => config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

const labelsWithFields = (value: string, labelFields: Array<string>, fieldTypes: FieldTypes) =>
  value.split(keySeparator).map((label, idx) => {
    const field = labelFields[idx];
    const type = fieldTypes?.currentQuery?.find((fieldType) => fieldType.name === field)?.type ?? FieldType.Unknown;

    return { label, field, type };
  });

const PlotLegend = ({
  children,
  config,
  chartData,
  labelMapper = defaultLabelMapper,
  labelFields = columnPivotsToFields,
  neverHide = false,
  height,
  width,
}: Props) => {
  const { columnPivots, series } = config;
  const { focusedWidget } = useContext(WidgetFocusContext);
  const interactive = useIsInteractiveMode();
  const fieldTypes = useContext(FieldTypesContext);
  const { limitHeight } = useContext(WidgetRenderingContext);
  const { colors, setColor } = useContext(ChartColorContext);
  const mapKeys = useMapKeys();

  const labels = labelMapper(chartData);
  const Container = limitHeight ? FixedContainer : VariableContainer;

  if (!neverHide && !focusedWidget?.editing && series.length <= 1 && columnPivots.length <= 0) {
    return (
      <Container $height={height} $width={width}>
        {children}
      </Container>
    );
  }

  const fields = labelFields(config);
  const items: Array<ChartLegendItem> = (interactive ? labels.sort(stringLenSort) : labels).map((value) => {
    const labelsWithField = labelsWithFields(value, fields, fieldTypes);

    return {
      key: value,
      color: colors.get(value, value === eventsDisplayName ? EVENT_COLOR : undefined),
      title: labelsWithField.map(({ label, field }) => mapKeys(label, field)).join(humanSeparator),
      label: labelsWithField.map(({ label, field, type }) =>
        field ? <Value key={`${field}:${label}`} type={type} value={label} field={field} /> : label,
      ),
    };
  });

  return (
    <Container $height={height} $width={width}>
      {children}
      <ChartLegend
        items={items}
        layout={interactive ? 'grid' : 'wrap'}
        colorPicker={interactive ? { colors: defaultColors, onChange: setColor } : undefined}
      />
    </Container>
  );
};

export default PlotLegend;
