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

import { Spinner } from 'components/common';
import { NodeLoggers } from 'components/loggers';
import useLoggers from 'components/loggers/useLoggers';
import useSubsystems from 'components/loggers/useSubsystems';

const LoggerOverview = () => {
  const { data: loggers } = useLoggers();
  const { data: subsystems } = useSubsystems();

  if (!loggers || !subsystems) {
    return <Spinner />;
  }

  const nodeLoggers = Object.keys(loggers)
    .map((nodeId) => (
      <NodeLoggers key={`node-loggers-${nodeId}`}
                   nodeId={nodeId}
                   subsystems={subsystems[nodeId] ? subsystems[nodeId].subsystems : {}} />
    ));

  return (
    <span>
      {nodeLoggers}
    </span>
  );
};

export default LoggerOverview;
