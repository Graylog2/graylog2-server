// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { TimeRange } from 'views/logic/queries/Query';

import EmptyAggregationContent from './EmptyAggregationContent';
import FullSizeContainer from './FullSizeContainer';

const defaultVisualizationType = 'table';

type OnVisualizationConfigChange = (VisualizationConfig) => void;

type Result = {
  total: number,
  rows: Rows,
  effective_timerange: TimeRange,
};

type Props = {
  config: AggregationWidgetConfig,
  data: { [string]: Result },
  editing?: boolean,
  toggleEdit: () => void,
  fields: FieldTypeMappingsList,
  onVisualizationConfigChange: OnVisualizationConfigChange,
};

export type VisualizationComponentProps = {|
  config: AggregationWidgetConfig,
  data: { [string]: Rows },
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

const AggregationBuilder = ({ config, data, editing = false, fields, onVisualizationConfigChange = () => {}, toggleEdit }: Props) => {
  if (!config || config.isEmpty) {
    return <EmptyAggregationContent toggleEdit={toggleEdit} editing={editing} />;
  }

  const VisComponent = _visualizationForType(config.visualization || defaultVisualizationType);
  const { effective_timerange: effectiveTimerange } = data && data.chart ? data.chart : {};
  const rows = Object.entries(data)
    .map(
      // $FlowFixMe: map claims it's `mixed`, we know it's `Result`
      ([key, value]: [string, Result]) => [
        key,
        value.rows,
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
