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
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Popover } from 'components/bootstrap';
import { OverlayTrigger, Icon } from 'components/common';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import UserDateTimeContext from 'contexts/UserDateTimeContext';
import { relativeDifference } from 'util/DateTime';

import style from './StatusIndicator.css';

type Props = {
  message: string,
  status: number,
  lastSeen: string,
  id: string,
}

const StatusIndicator = ({ message: messageProp, status, lastSeen, id }: Props) => {
  const { toUserTimezone } = useContext(UserDateTimeContext);
  let message = messageProp;
  const text = lodash.upperFirst(SidecarStatusEnum.toString(status));
  const lastSeenDateTime = toUserTimezone(lastSeen);

  let icon;
  let className;

  switch (status) {
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
      message += ` (${relativeDifference(lastSeenDateTime)})`;
  }

  if (message && id) {
    const popover = (
      <Popover id={`${id}-status-tooltip`}>
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
};

StatusIndicator.propTypes = {
  id: PropTypes.string,
  lastSeen: PropTypes.string,
  message: PropTypes.string,
  status: PropTypes.number,
};

StatusIndicator.defaultProps = {
  id: '',
  lastSeen: undefined,
  message: '',
  status: -1,
};

export default StatusIndicator;
