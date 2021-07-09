/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

import { Popover } from 'components/graylog';

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
