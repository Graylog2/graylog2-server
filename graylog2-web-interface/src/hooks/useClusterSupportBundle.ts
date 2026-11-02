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
import { useState } from 'react';
import type { QueryObserverResult } from '@tanstack/react-query';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { ClusterDebugSupportBundle } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { defaultOnError } from 'util/conditional/onError';
import i18n from 'i18n';

export type BundleFile = {
  size: number;
  file_name: string;
};

const fetchSupportBundleList = async () => ClusterDebugSupportBundle.listBundles();

const createSupportBundle = async (
  refetchList: () => Promise<QueryObserverResult<any, unknown>>,
  setLoading: (loading: boolean) => void,
) => {
  try {
    setLoading(true);
    await ClusterDebugSupportBundle.buildBundle();
    await refetchList();
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('use_cluster_support_bundle_notifications.create_failed', { ns: 'system', error: errorThrown }),
      i18n.t('use_cluster_support_bundle_notifications.create_failed_title', { ns: 'system' }),
    );
  } finally {
    setLoading(false);
  }
};

const deleteSupportBundle = async (filename: string, refetchList: () => Promise<QueryObserverResult<any, unknown>>) => {
  try {
    await ClusterDebugSupportBundle.remove(filename);
    await refetchList();
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('use_cluster_support_bundle_notifications.delete_failed', { ns: 'system', error: errorThrown }),
      i18n.t('use_cluster_support_bundle_notifications.delete_failed_title', { ns: 'system' }),
    );
  }
};

const downloadSupportBundle = async (filename: string) => {
  try {
    window.open(qualifyUrl(ApiRoutes.ClusterSupportBundleController.download(filename).url), '_self');
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('use_cluster_support_bundle_notifications.download_failed', { ns: 'system', error: errorThrown }),
      i18n.t('use_cluster_support_bundle_notifications.download_failed_title', { ns: 'system' }),
    );
  }
};

const useClusterSupportBundle = () => {
  const [isCreating, setIsCreating] = useState<boolean>(false);
  const { data, refetch } = useQuery({
    queryKey: ['supportBundleList', 'overview'],

    queryFn: () =>
      defaultOnError(
        fetchSupportBundleList(),
        i18n.t('use_cluster_support_bundle_notifications.load_list_failed', { ns: 'system' }),
        i18n.t('use_cluster_support_bundle_notifications.load_list_failed_title', { ns: 'system' }),
      ),

    placeholderData: keepPreviousData,
  });

  return {
    isCreating,
    list: data || [],
    onCreate: () => createSupportBundle(refetch, setIsCreating),
    onDelete: (filename: string) => deleteSupportBundle(filename, refetch),
    onDownload: downloadSupportBundle,
  };
};

export default useClusterSupportBundle;
