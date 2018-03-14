import React from 'react';
import PropTypes from 'prop-types';

import AggregationControls from './AggregationControls';
import EditModeToggleButton from './EditModeToggleButton';
import FullSizeContainer from './FullSizeContainer';
import DataTable from '../datatable/DataTable';

export default class AggregationBuilder extends React.Component {
  static defaultProps = {
    editing: false,
  };

  static propTypes = {
    editing: PropTypes.bool,
    fields: PropTypes.object.isRequired,
  };

  static _visualizationForType(type) {
    return DataTable;
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
    const VisComponent = AggregationBuilder._visualizationForType();
    const children = (
      <FullSizeContainer>
        <VisComponent {...this.props} />
      </FullSizeContainer>
    );
    const content = this.state.editing ? (
      <AggregationControls fields={this.props.fields} onChange={this.props.onChange} {...this.props.config}>
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
