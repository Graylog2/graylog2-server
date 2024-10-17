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
import upperFirst from 'lodash/upperFirst';

import { OverlayTrigger, Icon } from 'components/common';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import { relativeDifference } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';

import style from './StatusIndicator.css';

type Props = {
  message?: string
  status?: number
  lastSeen?: string
  id?: string
}

const StatusIndicator = ({ message: messageProp = '', status = -1, lastSeen, id = '' }: Props) => {
  const { toUserTimezone } = useUserDateTime();
  let message = messageProp;
  const text = upperFirst(SidecarStatusEnum.toString(status));
  const lastSeenDateTime = toUserTimezone(lastSeen);

  let icon;
  let className;

  switch (status) {
    case SidecarStatusEnum.RUNNING:
      className = 'text-success';
      icon = 'play_arrow';
      break;
    case SidecarStatusEnum.FAILING:
      className = 'text-danger';
      icon = 'warning';
      break;
    case SidecarStatusEnum.STOPPED:
      className = 'text-danger';
      icon = 'error';
      break;
    default:
      className = 'text-info';
      icon = 'help';
      message += ` (${relativeDifference(lastSeenDateTime)})`;
  }

  if (message && id) {
    return (
      <OverlayTrigger placement="top" overlay={message} rootClose trigger="hover">
        <span className={`${className} ${style.indicator}`}>
          <Icon name={icon} /> {text}
        </span>
      </OverlayTrigger>
    );
  }

  return (
    <span className={`${className} ${style.indicator}`}>
      <Icon name={icon} /> {text}
    </span>
  );
};

export default StatusIndicator;
