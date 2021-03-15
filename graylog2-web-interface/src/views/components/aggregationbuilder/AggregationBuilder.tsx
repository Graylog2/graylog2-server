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
import { PluginStore } from 'graylog-web-plugin/plugin';
import { WidgetComponentProps } from 'views/types';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { Events } from 'views/logic/searchtypes/events/EventHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import type VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';

import EmptyAggregationContent from './EmptyAggregationContent';
import FullSizeContainer from './FullSizeContainer';

const defaultVisualizationType = 'table';

type RowResult = {
  type: 'pivot',
  total: number,
  rows: Rows,
  effective_timerange: AbsoluteTimeRange,
};

type EventResult = {
  events: Events,
  type: 'events',
  name: 'events',
};

export type VisualizationComponentProps = {
  config: AggregationWidgetConfig,
  data: { [key: string]: Rows } & { events?: Events },
  editing?: boolean,
  effectiveTimerange: AbsoluteTimeRange,
  fields: FieldTypeMappingsList,
  height: number,
  onChange: OnVisualizationConfigChange,
  width: number,
  toggleEdit: () => void,
};

export type VisualizationComponent =
  { type: string, propTypes?: any }
  & React.ComponentType<VisualizationComponentProps>;

export const makeVisualization = (component: React.ComponentType<VisualizationComponentProps>, type: string): VisualizationComponent => {
  const visualizationComponent = component as VisualizationComponent;

  visualizationComponent.type = type;

  return visualizationComponent;
};

const _visualizationForType = (type: string): VisualizationComponent => {
  const visualizationTypes = PluginStore.exports('visualizationTypes');
  const visualization = visualizationTypes.filter((viz) => viz.type === type)[0];

  if (!visualization) {
    throw new Error(`Unable to find visualization component for type: ${type}`);
  }

  return visualization.component;
};

const getResult = (value: RowResult | EventResult): Rows | Events => {
  if (value.type === 'events') {
    return value.events;
  }

  return value.rows;
};

type OnVisualizationConfigChange = (newConfig: VisualizationConfig) => void;

type AggregationBuilderProps = WidgetComponentProps<AggregationWidgetConfig> & {
  onVisualizationConfigChange: OnVisualizationConfigChange,
};

const AggregationBuilder = ({ config, data, editing = false, fields, onVisualizationConfigChange = () => {}, toggleEdit }: AggregationBuilderProps) => {
  if (!config || config.isEmpty) {
    return <EmptyAggregationContent toggleEdit={toggleEdit} editing={editing} />;
  }

  const VisComponent = _visualizationForType(config.visualization || defaultVisualizationType);
  const { effective_timerange: effectiveTimerange } = data.chart || Object.values(data)[0] || {};
  const rows = Object.entries(data)
    .map((tuple) => tuple as ([string, RowResult] | ['events', EventResult]))
    .map(
      ([key, value]): [string, Rows | Events] => [key, getResult(value)],
    )
    .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});

  return (
    <FullSizeContainer>
      {({ height, width }) => (
        <VisComponent config={config}
                      data={rows}
                      effectiveTimerange={effectiveTimerange}
                      editing={editing}
                      fields={fields}
                      height={height}
                      width={width}
                      toggleEdit={toggleEdit}
                      onChange={onVisualizationConfigChange} />
      )}
    </FullSizeContainer>
  );
};

export default AggregationBuilder;
