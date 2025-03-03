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

import type FetchError from 'logic/errors/FetchError';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import { onError } from 'util/conditional/onError';

export const INPUT_REFERENCES = 'streams_by_index_set_id';

const DEFAULT_DATA = {
  stream_refs: [],
  pipeline_refs: [],
};

type ResponseData = {
  input_id: string;
  stream_refs: Array<{ id: string; name: string }>;
  pipeline_refs: Array<{ id: string; name: string }>;
};

const fetchInputReferences = async (inputId: string): Promise<ResponseData> => {
  const url = qualifyUrl(ApiRoutes.InputsApiController.references(inputId).url);

  return fetch('GET', url);
};

const useInputReferences = (
  inputId?: string,
): {
  data: {
    stream_refs: Array<{ id: string; name: string }>;
    pipeline_refs: Array<{ id: string; name: string }>;
    isInputAlreadyInUse: boolean;
  };
  isLoading: boolean;
} => {
  const { data, isLoading } = useQuery<ResponseData, FetchError>(
    [INPUT_REFERENCES],
    () =>
      onError(fetchInputReferences(inputId), (errorThrown: FetchError) => {
        if (!(errorThrown.status === 404)) {
          UserNotification.error(`Loading input references failed with: ${errorThrown}`);
        }
      }),
    { enabled: !!inputId },
  );

  const inputReferencesData = data ?? DEFAULT_DATA;

  const isInputAlreadyInUse =
    inputReferencesData.stream_refs.length > 0 || inputReferencesData.pipeline_refs.length > 0;

  return {
    data: {
      isInputAlreadyInUse,
      stream_refs: inputReferencesData.stream_refs,
      pipeline_refs: inputReferencesData.pipeline_refs,
    },
    isLoading,
  };
};

export default useInputReferences;
