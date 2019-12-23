// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { Events } from 'views/logic/searchtypes/events/EventHandler';
import type { TimeRange } from 'views/logic/queries/Query';

import EmptyAggregationContent from './EmptyAggregationContent';
import FullSizeContainer from './FullSizeContainer';
import type { OnVisualizationConfigChange, WidgetProps } from '../widgets/Widget';

const defaultVisualizationType = 'table';

type RowResult = {
  type: 'pivot',
  total: number,
  rows: Rows,
  effective_timerange: TimeRange,
};

type EventResult = {
  events: Events,
  type: 'events',
  name: 'events',
};

export type VisualizationComponentProps = {|
  config: AggregationWidgetConfig,
  data: { [string]: Rows, events?: Events },
  editing?: boolean,
  effectiveTimerange: TimeRange,
  fields: FieldTypeMappingsList,
  height: number,
  onChange: OnVisualizationConfigChange,
  width: number,
  toggleEdit: () => void,
|};

export type VisualizationComponent =
  { type?: string, propTypes?: any }
  & React.ComponentType<VisualizationComponentProps>;

const _visualizationForType = (type: string): VisualizationComponent => {
  const visualizationTypes = PluginStore.exports('visualizationTypes');
  const visualization = visualizationTypes.filter(viz => viz.type === type)[0];
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

const AggregationBuilder = ({ config, data, editing = false, fields, onVisualizationConfigChange = () => {}, toggleEdit }: WidgetProps) => {
  if (!config || config.isEmpty) {
    return <EmptyAggregationContent toggleEdit={toggleEdit} editing={editing} />;
  }

  const VisComponent = _visualizationForType(config.visualization || defaultVisualizationType);
  const { effective_timerange: effectiveTimerange } = data.chart || Object.values(data)[0] || {};
  const rows = Object.entries(data)
    .map(
      // $FlowFixMe: map claims it's `mixed`, we know it's `RowResult`
      ([key, value]: [string, RowResult] | ['events', EventResult]) => [
        key,
        getResult(value),
      ],
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

AggregationBuilder.defaultProps = {
  editing: false,
};

export default AggregationBuilder;
