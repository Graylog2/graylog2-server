import React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import { Popover } from 'react-bootstrap';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';
import PivotConfiguration from './PivotConfiguration';

export default class ConfigurablePivot extends React.Component {
  static propTypes = {};

  constructor(props, context) {
    super(props, context);
    this.state = {
      isOpen: false,
    };
  }

  _onClick = () => {
    this.setState({ isOpen: true });
  };

  _onClose = (config) => {
    this.setState({ isOpen: false });
    this.props.onChange(config);
  };

  render() {
    const { value, config } = this.props;
    const popover = this.state.isOpen && (
      <Portal>
        <Position
          container={document.body}
          placement="bottom"
          target={this.target}>
          <Popover title="Pivot Configuration">
            <PivotConfiguration field={value.value} config={config} onClose={this._onClose} />
          </Popover>
        </Position>
      </Portal>
    );
    return (
      <span>
        <Select.Value ref={(elem) => { this.target = elem; }} {...this.props} onClick={this._onClick}>
          {this.props.children}
        </Select.Value>
        {popover}
      </span>
    );
  }
};
