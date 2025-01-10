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
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';

const fetchDataNodeLogsStdout = async (hostname: string) => fetch('GET', qualifyUrl(`/datanodes/${hostname}/rest/logs/stdout`));
const fetchDataNodeLogsStderr = async (hostname: string) => fetch('GET', qualifyUrl(`/datanodes/${hostname}/rest/logs/stderr`));

const useDataNodeLogs = (hostname: string, enabled: boolean) : {
  stdout: string[],
  stderr: string[],
} => {
  const { data: stdout } = useQuery(
    ['datanode_stdout_logs'],
    () => defaultOnError(fetchDataNodeLogsStdout(hostname), 'Loading Data Node stdout logs failed with status', 'Could not load Data Node stdout logs'),
    {
      enabled,
    },
  );

  const { data: stderr } = useQuery(
    ['datanode_stderr_logs'],
    () => defaultOnError(fetchDataNodeLogsStderr(hostname), 'Loading Data Node stderr logs failed with status', 'Could not load Data Node stderr logs'),
    {
      enabled,
    },
  );

  return ({
    stdout,
    stderr,
  });
};

export default useDataNodeLogs;
