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

import { Alert } from 'components/bootstrap';
import DataNodeRestartProgress from 'components/datanode/configuration/DataNodeRestartProgress';
import useDataNodeRestartProgress from 'components/datanode/configuration/useDataNodeRestartProgress';

/**
 * Tells that Data Nodes are being restarted, whether one at a time from its settings page or all of them one after
 * another, so their metrics and status in the table below are expected to be off for a while.
 */
const DataNodeRestartBanner = () => {
  const { isRestarting, isPaused, progress } = useDataNodeRestartProgress(null);

  if (!isRestarting) return null;

  return (
    <Alert
      bsStyle={isPaused ? 'warning' : 'info'}
      title={isPaused ? 'Data Node restart paused' : 'Data Node restart in progress'}>
      <DataNodeRestartProgress progress={progress} paused={isPaused} />
    </Alert>
  );
};

export default DataNodeRestartBanner;
