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

import { PipelinesPipelines } from '@graylog/server-api';

import { type PipelineType } from 'stores/pipelines/PipelinesStore';
import type FetchError from 'logic/errors/FetchError';
import { defaultOnError } from 'util/conditional/onError';

type Options = {
  enabled: boolean,
}

const usePipelines = ({ enabled }: Options = { enabled: true }) : {
  data: Array<PipelineType>,
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery<Array<PipelineType>, FetchError>(
    ['pipelines'],
    () => defaultOnError(PipelinesPipelines.getAll(), 'Loading pipelines failed with status', 'Could not load pipelines'),
    {
      enabled,
    },
  );

  return ({
    data: data ?? [],
    refetch,
    isInitialLoading,
  });
};

export default usePipelines;
