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
import lodash from 'lodash';

import { OverlayTrigger, Popover } from 'components/graylog';
import { Icon } from 'components/common';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import DateTime from 'logic/datetimes/DateTime';

import style from './StatusIndicator.css';

class StatusIndicator extends React.Component {
  static propTypes = {
    id: PropTypes.string,
    lastSeen: PropTypes.string,
    message: PropTypes.string,
    status: PropTypes.number,
  };

  static defaultProps = {
    id: '',
    lastSeen: undefined,
    message: '',
    status: -1,
  };

  render() {
    const text = lodash.upperFirst(SidecarStatusEnum.toString(this.props.status));
    const lastSeenDateTime = new DateTime(this.props.lastSeen);

    let icon;
    let className;
    let { message } = this.props;

    switch (this.props.status) {
      case SidecarStatusEnum.RUNNING:
        className = 'text-success';
        icon = 'play';
        break;
      case SidecarStatusEnum.FAILING:
        className = 'text-danger';
        icon = 'exclamation-triangle';
        break;
      case SidecarStatusEnum.STOPPED:
        className = 'text-danger';
        icon = 'stop';
        break;
      default:
        className = 'text-info';
        icon = 'question-circle';
        message += ` (${lastSeenDateTime.toRelativeString()})`;
    }

    if (this.props.message && this.props.id) {
      const popover = (
        <Popover id={`${this.props.id}-status-tooltip`}>
          {message}
        </Popover>
      );

      return (
        <OverlayTrigger placement="top" overlay={popover} rootClose trigger="hover">
          <span className={`${className} ${style.indicator}`}>
            <Icon name={icon} fixedWidth /> {text}
          </span>
        </OverlayTrigger>
      );
    }

    return (
      <span className={`${className} ${style.indicator}`}>
        <Icon name={icon} fixedWidth /> {text}
      </span>
    );
  }
}

export default StatusIndicator;
