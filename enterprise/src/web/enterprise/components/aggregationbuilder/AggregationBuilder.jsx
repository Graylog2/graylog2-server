import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { AggregationType } from './AggregationBuilderPropTypes';
import AggregationControls from './AggregationControls';
import EditModeToggleButton from './EditModeToggleButton';
import FullSizeContainer from './FullSizeContainer';
import DataTable from '../datatable/DataTable';

const defaultVisualizationType = 'table';

export default class AggregationBuilder extends React.Component {
  static defaultProps = {
    editing: false,
  };

  static propTypes = {
    editing: PropTypes.bool,
    fields: PropTypes.object.isRequired,
  };

  static _visualizationForType(type) {
    const visualizationTypes = PluginStore.exports('visualizationTypes');
    const visualization = (visualizationTypes.filter(viz => viz.type === type)[0] || {});
    return visualization.component;
  }

  constructor(props) {
    super(props);
    this.state = {
      editing: props.editing,
    };
  }

  _toggleEditMode = () => {
    this.setState(state => ({ editing: !state.editing }));
  };

  render() {
    const { config, fields, onChange } = this.props;
    const VisComponent = AggregationBuilder._visualizationForType(config.visualization || defaultVisualizationType);
    const children = (
      <FullSizeContainer>
        <VisComponent {...this.props} />
      </FullSizeContainer>
    );
    const content = this.state.editing ? (
      <AggregationControls fields={fields} onChange={onChange} {...config}>
        {children}
      </AggregationControls>
    ) : children;
    return (
      <span>
        <EditModeToggleButton value={this.state.editing} onToggle={this._toggleEditMode} />
        {content}
      </span>
    );
  }
}

AggregationBuilder.propTypes = {
  onChange: PropTypes.func.isRequired,
  config: AggregationType,
};

AggregationBuilder.defaultProps = {
  config: {
    visualization: defaultVisualizationType,
  },
};
