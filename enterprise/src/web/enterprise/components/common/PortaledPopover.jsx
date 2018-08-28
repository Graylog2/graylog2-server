import React from 'react';
import PropTypes from 'prop-types';
import { Popover } from 'react-bootstrap';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';
import CustomPropTypes from '../CustomPropTypes';

export default class PortaledPopover extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.string,
    ]).isRequired,
    popover: CustomPropTypes.OneOrMoreChildren.isRequired,
    title: PropTypes.string,
  };

  static defaultProps = {
    title: null,
  };

  state = {
    isOpen: false,
  };

  _onClick = () => this.setState(state => ({ isOpen: !state.isOpen }));

  render() {
    const { popover, title, ...rest } = this.props;
    const popoverElem = this.state.isOpen && (
      <Portal>
        <Position
          container={document.body}
          placement="bottom"
          target={this.target}>
          <Popover title={title} id={title}>
            {popover}
          </Popover>
        </Position>
      </Portal>
    );
    return (
      <span>
        <a role="link" tabIndex={0} ref={(elem) => {
          this.target = elem;
        }} {...rest} onClick={this._onClick}>
          {this.props.children}
        </a>
        {popoverElem}
      </span>
    );
  }
}
