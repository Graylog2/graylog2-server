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
import PropTypes from 'prop-types';
import React from 'react';

import { OverlayTrigger, Popover } from 'components/graylog';
import { Icon } from 'components/common';

import Styles from './ErrorPopover.css';

class ErrorPopover extends React.Component {
  static propTypes = {
    errorText: PropTypes.string.isRequired,
    title: PropTypes.string,
    placement: PropTypes.string,
  };

  static defaultProps = {
    title: 'Error',
    placement: 'bottom',
  };

  render() {
    const overlay = (
      <Popover id="error-popover" title={this.props.title} className={Styles.overlay}>
        {this.props.errorText}
      </Popover>
    );

    return (
      <OverlayTrigger trigger={['hover', 'focus']} placement={this.props.placement} overlay={overlay}>
        <span className={Styles.trigger}>
          <Icon name="exclamation-triangle" className="text-danger" />
        </span>
      </OverlayTrigger>
    );
  }
}

export default ErrorPopover;
