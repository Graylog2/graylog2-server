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
import { useQuery } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const fetchDataNodeLogsStdout = async (hostname: string) => fetch('GET', qualifyUrl(`/datanodes/${hostname}/rest/logs/stdout`));
const fetchDataNodeLogsStderr = async (hostname: string) => fetch('GET', qualifyUrl(`/datanodes/${hostname}/rest/logs/stderr`));

const useDataNodeLogs = (hostname: string) : {
  stdout: string[],
  stderr: string[],
} => {
  const { data: stdout } = useQuery(
    ['datanode_stdout_logs'],
    () => fetchDataNodeLogsStdout(hostname),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Data Node stdout logs failed with status: ${errorThrown}`,
          'Could not load Data Node stdout logs');
      },
    },
  );

  const { data: stderr } = useQuery(
    ['datanode_stderr_logs'],
    () => fetchDataNodeLogsStderr(hostname),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Data Node stderr logs failed with status: ${errorThrown}`,
          'Could not load Data Node stderr logs');
      },
    },
  );

  return ({
    stdout,
    stderr,
  });
};

export default useDataNodeLogs;
