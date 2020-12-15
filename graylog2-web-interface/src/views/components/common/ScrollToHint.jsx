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

import UIUtils from 'util/UIUtils';

export default class ScrollToHint extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    value: PropTypes.any.isRequired,
  };

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (!this.element) {
      return;
    }

    if (this.props.value !== nextProps.value) {
      UIUtils.scrollToHint(this.element);
    }
  }

  render() {
    const { children } = this.props;

    return (
      <span ref={(element) => { this.element = element; }}>
        {children}
      </span>
    );
  }
}
