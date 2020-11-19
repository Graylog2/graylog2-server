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
// @flow strict
import * as React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

const MeasureDimensions = createReactClass({
  propTypes: {
    children: PropTypes.node.isRequired,
  },

  getInitialState() {
    return {
      height: undefined,
    };
  },

  componentDidMount() {
    window.addEventListener('resize', this._setHeight);
    this._setHeight();
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._setHeight);
  },

  _setHeight() {
    this.setState({ height: this._getHeight() });
  },

  _getHeight() {
    if (this.container) {
      return this.container.offsetHeight;
    }

    return undefined;
  },

  _renderChildren() {
    return React.Children.map(this.props.children, (child) => {
      return React.cloneElement(child, {
        containerHeight: this.state.height,
      });
    });
  },

  render() {
    return (
      <span ref={(node) => { this.container = node; }} style={{ display: 'block', height: '100%' }}>
        {this._renderChildren()}
      </span>
    );
  },
});

export default MeasureDimensions;
