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

import fetch, { fetchStreamingPlainText } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { SystemOverview } from 'stores/cluster/types';

const SOURCE_URL = '/cluster';

export const fetchClusterOverview = (): Promise<{ [nodeId: string]: SystemOverview }> =>
  fetch('GET', qualifyUrl(SOURCE_URL));

export const fetchNodeJvm = (nodeId: string) => {
  const promise = fetch('GET', qualifyUrl(`${SOURCE_URL}/${nodeId}/jvm`));

  promise.catch((error) =>
    UserNotification.error(
      `Getting JVM information for node '${nodeId}' failed: ${error}`,
      'Could not get JVM information',
    ),
  );

  return promise;
};

export const fetchThreadDump = (nodeId: string): Promise<string> =>
  fetch('GET', qualifyUrl(`${SOURCE_URL}/${nodeId}/threaddump`)).then(
    (response) => response.threaddump,
    (error) => {
      UserNotification.error(`Getting thread dump for node '${nodeId}' failed: ${error}`, 'Could not get thread dump');

      throw error;
    },
  );

export const fetchProcessBufferDump = (nodeId: string) =>
  fetch('GET', qualifyUrl(`${SOURCE_URL}/${nodeId}/processbufferdump`)).then(
    (response) => response.processbuffer_dump,
    (error) => {
      UserNotification.error(
        `Getting process buffer dump for node '${nodeId}' failed: ${error}`,
        'Could not get process buffer dump',
      );

      throw error;
    },
  );

export const fetchSystemLogs = (nodeId: string, limit: number) =>
  fetchStreamingPlainText(
    'GET',
    qualifyUrl(`${SOURCE_URL}/system/loggers/messages/recent/${nodeId}?limit=${limit}`),
  ).then(
    (response) => response,
    (error) => {
      UserNotification.error(
        `Getting system log messages for node '${nodeId}' failed: ${error}`,
        'Could not get system log messages',
      );

      throw error;
    },
  );

const useClusterOverview = () =>
  useQuery({
    queryKey: ['cluster', 'overview'],
    queryFn: () =>
      fetchClusterOverview().catch((error) => {
        UserNotification.error(`Getting cluster overview failed: ${error}`, 'Could not get cluster overview');

        throw error;
      }),
    refetchInterval: 5000,
  });

export default useClusterOverview;
