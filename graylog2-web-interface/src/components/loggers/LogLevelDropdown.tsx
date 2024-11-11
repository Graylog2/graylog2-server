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
import { useCallback } from 'react';
import capitalize from 'lodash/capitalize';

import { DropdownButton, MenuItem } from 'components/bootstrap';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { availableLoglevels } from 'components/loggers/Constants';
import useSetSubsystemLoggerLevel from 'components/loggers/useSetSubsystemLoggerLevel';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';

type Props = {
  name: string,
  nodeId: string,
  subsystem: { level: string },
}

const LogLevelDropdown = ({ nodeId, name, subsystem }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();
  const { setSubsystemLoggerLevel } = useSetSubsystemLoggerLevel();

  const _changeLoglevel = useCallback((loglevel: string) => setSubsystemLoggerLevel(nodeId, name, loglevel), [name, nodeId, setSubsystemLoggerLevel]);

  const _menuLevelClick = useCallback((loglevel: string) => () => {
    _changeLoglevel(loglevel);

    sendTelemetry(TELEMETRY_EVENT_TYPE.LOGGING.LOG_LEVEL_EDITED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'log-level-change',
      event_details: { value: loglevel },
    });
  }, [_changeLoglevel, location?.pathname, sendTelemetry]);

  const loglevels = availableLoglevels
    .map((loglevel) => (
      <MenuItem key={`${subsystem}-${nodeId}-${loglevel}`}
                active={subsystem.level === loglevel}
                onClick={_menuLevelClick(loglevel)}>
        {capitalize(loglevel)}
      </MenuItem>
    ));

  return (
    <DropdownButton id="loglevel" bsSize="xsmall" title={capitalize(subsystem.level)}>
      {loglevels}
    </DropdownButton>
  );
};

export default LogLevelDropdown;
