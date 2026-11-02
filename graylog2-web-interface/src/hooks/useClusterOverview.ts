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

import { Cluster } from '@graylog/server-api';

import fetch, { fetchStreamingPlainText } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { SystemOverview } from 'stores/cluster/types';
import i18n from 'i18n';

const SOURCE_URL = '/cluster';

export const fetchClusterOverview = (): Promise<{ [nodeId: string]: SystemOverview }> => Cluster.get();

export const fetchNodeJvm = (nodeId: string) => {
  const promise = fetch('GET', qualifyUrl(`${SOURCE_URL}/${nodeId}/jvm`));

  promise.catch((error) =>
    UserNotification.error(
      i18n.t('use_cluster_overview_notifications.jvm_failed', { ns: 'system', nodeId, error }),
      i18n.t('use_cluster_overview_notifications.jvm_failed_title', { ns: 'system' }),
    ),
  );

  return promise;
};

export const fetchThreadDump = (nodeId: string): Promise<string> =>
  Cluster.threadDump(nodeId).then(
    (response) => response.threaddump,
    (error) => {
      UserNotification.error(
        i18n.t('use_cluster_overview_notifications.thread_dump_failed', { ns: 'system', nodeId, error }),
        i18n.t('use_cluster_overview_notifications.thread_dump_failed_title', { ns: 'system' }),
      );

      throw error;
    },
  );

export const fetchProcessBufferDump = (nodeId: string) =>
  Cluster.processBufferDump(nodeId).then(
    (response) => response.processbuffer_dump,
    (error) => {
      UserNotification.error(
        i18n.t('use_cluster_overview_notifications.process_buffer_dump_failed', { ns: 'system', nodeId, error }),
        i18n.t('use_cluster_overview_notifications.process_buffer_dump_failed_title', { ns: 'system' }),
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
        i18n.t('use_cluster_overview_notifications.system_logs_failed', { ns: 'system', nodeId, error }),
        i18n.t('use_cluster_overview_notifications.system_logs_failed_title', { ns: 'system' }),
      );

      throw error;
    },
  );

const useClusterOverview = () =>
  useQuery({
    queryKey: ['cluster', 'overview'],
    queryFn: () =>
      fetchClusterOverview().catch((error) => {
        UserNotification.error(i18n.t('use_cluster_overview_notifications.overview_failed', { ns: 'system', error }), i18n.t('use_cluster_overview_notifications.overview_failed_title', { ns: 'system' }));

        throw error;
      }),
    refetchInterval: 5000,
  });

export default useClusterOverview;
