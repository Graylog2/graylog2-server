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

type InputType = {
  requested_configuration: {
    [key: string]: {
      [key: string]: object;
    };
  };
  link_to_docs: string;
  name: string;
  is_exclusive: boolean;
  description: string;
  type: string;
};
export type InputTypeDescriptionsResponse = {
  [_key: string]: InputType;
};
type Options = {
  enabled: boolean;
};

export const fetchInputTypes = async (): Promise<InputTypeDescriptionsResponse> => await SystemInputsTypes.all();

const useInputTypesDescriptions = (
  { enabled }: Options = { enabled: true },
): {
  data: InputTypeDescriptionsResponse;
  refetch: () => void;
  isLoading: boolean;
} => {
  const { data, refetch, isLoading } = useQuery<InputTypeDescriptionsResponse, Error>({
    queryKey: ['inputTypes', 'all'],
    queryFn: () =>
      defaultOnError<InputTypeDescriptionsResponse>(
        fetchInputTypes(),
        'Loading input types descriptions failed with status',
        'Could not load input types descriptions',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return { data: data || {}, refetch, isLoading };
};

export default useInputTypesDescriptions;
