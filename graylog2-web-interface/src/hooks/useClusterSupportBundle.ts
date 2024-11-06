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
import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { defaultOnError } from 'util/conditional/onError';

export type BundleFile = {
  size: number;
  file_name: string;
}

const fetchSupportBundleList = async () => fetch('GET', qualifyUrl(ApiRoutes.ClusterSupportBundleController.list().url));

const createSupportBundle = async (refetchList: () => Promise<QueryObserverResult<any, unknown>>, setLoading: (loading: boolean) => void) => {
  try {
    setLoading(true);
    await fetch('POST', qualifyUrl(ApiRoutes.ClusterSupportBundleController.create().url));
    await refetchList();
  } catch (errorThrown) {
    UserNotification.error(`Creating the Support Bundle failed with status: ${errorThrown}`, 'Could not create the Support Bundle.');
  } finally {
    setLoading(false);
  }
};

const deleteSupportBundle = async (filename: string, refetchList: () => Promise<QueryObserverResult<any, unknown>>) => {
  try {
    await fetch('DELETE', qualifyUrl(ApiRoutes.ClusterSupportBundleController.delete(filename).url));
    await refetchList();
  } catch (errorThrown) {
    UserNotification.error(`Deleting the Support Bundle failed with status: ${errorThrown}`, 'Could not delete the Support Bundle.');
  }
};

const downloadSupportBundle = async (filename: string) => {
  try {
    window.open(qualifyUrl(ApiRoutes.ClusterSupportBundleController.download(filename).url), '_self');
  } catch (errorThrown) {
    UserNotification.error(`Downloading the Support Bundle failed with status: ${errorThrown}`, 'Could not download the Support Bundle.');
  }
};

const useClusterSupportBundle = () => {
  const [isCreating, setIsCreating] = useState<boolean>(false);
  const { data, refetch } = useQuery<BundleFile[]>(
    ['supportBundleList', 'overview'],
    () => defaultOnError(fetchSupportBundleList(), 'Loading Support Bundle list failed with status', 'Could not load Support Bundle list.'),
    {
      keepPreviousData: true,
    },
  );

  return {
    isCreating,
    list: data || [],
    onCreate: () => createSupportBundle(refetch, setIsCreating),
    onDelete: (filename: string) => deleteSupportBundle(filename, refetch),
    onDownload: downloadSupportBundle,
  };
};

export default useClusterSupportBundle;
