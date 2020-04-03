import React from 'react';
import PropTypes from 'prop-types';
import { Popover } from 'components/graylog';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';
import CustomPropTypes from '../CustomPropTypes';

export default class PortaledPopover extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      CustomPropTypes.OneOrMoreChildren,
      PropTypes.string,
    ]).isRequired,
    container: PropTypes.any,
    popover: CustomPropTypes.OneOrMoreChildren.isRequired,
    title: PropTypes.string,
  };

  static defaultProps = {
    container: document.body,
    title: null,
  };

  state = {
    isOpen: false,
  };

  _onClick = () => this.setState((state) => ({ isOpen: !state.isOpen }));

  render() {
    const { container, popover, title, ...rest } = this.props;
    const popoverElem = this.state.isOpen && (
      <Portal node={container}>
        <Position container={container}
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
        <a role="link"
           tabIndex={0}
           ref={(elem) => {
             this.target = elem;
           }}
           {...rest}
           onClick={this._onClick}>
          {this.props.children}
        </a>
        {popoverElem}
      </span>
    );
  }
}
