// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationConfig from 'enterprise/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { AggregationType } from './AggregationBuilderPropTypes';
import FullSizeContainer from './FullSizeContainer';

const defaultVisualizationType = 'table';

type Props = {
  config: AggregationWidgetConfig,
  data: {
    total: number,
    rows: Array<any>,
  },
  editing: boolean,
  fields: {},
  onChange: () => void,
  onVisualizationConfigChange: (config: VisualizationConfig) => void,
};

export default class AggregationBuilder extends React.Component<Props> {
  static defaultProps = {
    editing: false,
    onChange: () => {},
    visualization: defaultVisualizationType,
    onVisualizationConfigChange: () => {},
  };

  static propTypes = {
    config: AggregationType.isRequired,
    data: PropTypes.shape({
      total: PropTypes.number.isRequired,
    }).isRequired,
    editing: PropTypes.bool,
    fields: PropTypes.object.isRequired,
    onChange: PropTypes.func,
  };

  static _visualizationForType(type: string) {
    const visualizationTypes = PluginStore.exports('visualizationTypes');
    const visualization = visualizationTypes.filter(viz => viz.type === type)[0];
    if (!visualization) {
      throw new Error(`Unable to find visualization component for type: ${type}`);
    }
    return visualization.component;
  }

  render() {
    const { config, data, onVisualizationConfigChange } = this.props;
    const VisComponent = AggregationBuilder._visualizationForType(config.visualization || defaultVisualizationType);
    const { rows } = data;
    return (
      <FullSizeContainer>
        <VisComponent {...this.props} data={rows} onChange={onVisualizationConfigChange} />
      </FullSizeContainer>
    );
  }
}
