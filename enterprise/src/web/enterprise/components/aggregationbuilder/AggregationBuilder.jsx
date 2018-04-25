import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { AggregationType } from './AggregationBuilderPropTypes';
import AggregationControls from './AggregationControls';
import FullSizeContainer from './FullSizeContainer';

const defaultVisualizationType = 'table';

export default class AggregationBuilder extends React.Component {
  static defaultProps = {
    editing: false,
    visualization: defaultVisualizationType,
  };

  static propTypes = {
    config: AggregationType.isRequired,
    data: PropTypes.array.isRequired,
    editing: PropTypes.bool,
    fields: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static _visualizationForType(type) {
    const visualizationTypes = PluginStore.exports('visualizationTypes');
    const visualization = (visualizationTypes.filter(viz => viz.type === type)[0] || {});
    return visualization.component;
  }

  render() {
    const { config, data, fields, onChange } = this.props;
    const VisComponent = AggregationBuilder._visualizationForType(config.visualization || defaultVisualizationType);
    const chartData = data && data[0] ? data : [{ results: [] }];
    const children = (
      <FullSizeContainer>
        <VisComponent {...this.props} data={chartData} />
      </FullSizeContainer>
    );
    const content = this.props.editing ? (
      <AggregationControls fields={fields} onChange={onChange} {...config.toObject()}>
        {children}
      </AggregationControls>
    ) : children;
    return (
      <span>
        {content}
      </span>
    );
  }
}
