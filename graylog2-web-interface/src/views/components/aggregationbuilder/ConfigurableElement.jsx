import React from 'react';
import PropTypes from 'prop-types';
import { Popover } from 'components/graylog';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

import CustomPropTypes from '../CustomPropTypes';

import styles from './ConfigurableElement.css';

export default class ConfigurableElement extends React.Component {
  static propTypes = {
    children: CustomPropTypes.OneOrMoreChildren.isRequired,
    configuration: CustomPropTypes.OneOrMoreChildren.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      isOpen: false,
    };
  }

  _onClick = () => {
    this.setState((state) => ({ isOpen: !state.isOpen }));
  };

  _onClose = (config) => {
    this.setState({ isOpen: false });
    this.props.onChange(config);
  };

  _stopEvent = (e) => {
    e.stopPropagation();
  };

  render() {
    const ConfigurationElement = this.props.configuration;
    const { title } = this.props;
    const popover = this.state.isOpen && (
      <Portal>
        <Position container={document.body}
                  placement="bottom"
                  target={this.target}>
          <Popover title={title} id="configuration-popover">
            <ConfigurationElement onClose={this._onClose} />
          </Popover>
        </Position>
      </Portal>
    );
    return (
      <span onMouseDown={this._stopEvent} onKeyDown={this._stopEvent}>
        <span ref={(elem) => { this.target = elem; }} onClick={this._onClick} className={styles.labelAsLink}>
          {this.props.children}
        </span>
        {popover}
      </span>
    );
  }
}
