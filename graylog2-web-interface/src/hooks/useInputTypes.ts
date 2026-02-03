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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { SystemInputsTypes } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

type Options = {
  enabled: boolean;
};
export type InputTypesSummary = {
  types: {
    [key: string]: string;
  };
};

export const fetchInputTypes = async (): Promise<InputTypesSummary> => await SystemInputsTypes.types();

const useInputTypes = (
  { enabled }: Options = { enabled: true },
): {
  data: InputTypesSummary;
  refetch: () => void;
  isLoading: boolean;
} => {
  const { data, refetch, isLoading } = useQuery<InputTypesSummary, Error>({
    queryKey: ['inputTypes', 'types'],
    queryFn: () =>
      defaultOnError<InputTypesSummary>(
        fetchInputTypes(),
        'Loading input types failed with status',
        'Could not load input types',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return { data: data || { types: {} }, refetch, isLoading };
};

export default useInputTypes;
