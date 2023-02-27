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
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { WidgetComponentProps } from 'views/types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { Events } from 'views/logic/searchtypes/events/EventHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import type { OnVisualizationConfigChange } from 'views/components/aggregationwizard/OnVisualizationConfigChangeContext';
import OnVisualizationConfigChangeContext from 'views/components/aggregationwizard/OnVisualizationConfigChangeContext';

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

type VisualizationResult = { [key: string]: Rows } & { events?: Events };

export type VisualizationComponentProps = {
  config: AggregationWidgetConfig,
  data: VisualizationResult,
  editing?: boolean,
  effectiveTimerange: AbsoluteTimeRange,
  fields: FieldTypeMappingsList,
  height: number,
  onChange: OnVisualizationConfigChange,
  width: number,
  toggleEdit: () => void,
};

export type VisualizationComponent<T extends string> =
  { type: T, propTypes?: any }
  & React.ComponentType<VisualizationComponentProps>;

export const retrieveChartData = (data: VisualizationResult): Rows => data.chart ?? data[Object.keys(data).filter((name) => name !== 'events')[0]];

export const makeVisualization = <T extends string, P extends VisualizationComponentProps> (component: React.ComponentType<P>, type: T): VisualizationComponent<T> => Object.assign(component, { type });

const _visualizationForType = <T extends string> (type: T): VisualizationComponent<T> => {
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

const AggregationBuilder = ({
  config,
  data,
  editing = false,
  fields,
  toggleEdit,
}: WidgetComponentProps<AggregationWidgetConfig>) => {
  const onVisualizationConfigChange = useContext(OnVisualizationConfigChangeContext);

  if (!config || config.isEmpty) {
    return <EmptyAggregationContent toggleEdit={toggleEdit} editing={editing} />;
  }

  const VisComponent = _visualizationForType(config.visualization || defaultVisualizationType);
  const { effective_timerange: effectiveTimerange } = data.chart || Object.values(data)[0] || {};

  const rows = Object.fromEntries(
    Object.entries(data)
      .map((tuple) => tuple as ([string, RowResult] | ['events', EventResult]))
      .map(
        ([key, value]): [string, Rows | Events] => [key, getResult(value)],
      ),
  ) as VisualizationResult;

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
