import React from 'react';
import PropTypes from 'prop-types';

import AggregationControls from './AggregationControls';
import EditModeToggleButton from './EditModeToggleButton';
import FullSizeContainer from './FullSizeContainer';

export default class AggregationBuilder extends React.Component {
  static defaultProps = {
    editing: false,
  };

  static propTypes = {
    children: PropTypes.element.isRequired,
    editing: PropTypes.bool,
  };

  constructor(props) {
    super(props);
    this.state = {
      editing: props.editing,
    };
    this._toggleEditMode = this._toggleEditMode.bind(this);
  }

  _toggleEditMode() {
    this.setState(state => ({ editing: !state.editing }));
  }

  render() {
    const children = (
      <FullSizeContainer>
        {this.props.children}
      </FullSizeContainer>
    );
    const content = this.state.editing ? (
      <AggregationControls fields={this.props.fields}>
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
