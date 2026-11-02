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

import { Datanode } from '@graylog/server-api';

import type { DataNode } from 'components/datanode/Types';
import { defaultOnError } from 'util/conditional/onError';

const fetchDataNode = async (datanodeId: string) => Datanode.getDataNode(datanodeId);

const useDataNode = (
  datanodeId: string,
): {
  data: DataNode;
  refetch: () => void;
  isInitialLoading: boolean;
  error: any;
} => {
  const { data, refetch, isInitialLoading, error } = useQuery({
    queryKey: ['datanode', datanodeId],
    queryFn: () =>
      defaultOnError(fetchDataNode(datanodeId), 'Loading Data Node failed with status', 'Could not load Data Node'),
    notifyOnChangeProps: ['data', 'error'],
    refetchInterval: 5000,
  });

  return {
    data,
    refetch,
    isInitialLoading,
    error,
  };
};

export default useDataNode;
