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
import type { QueryClient } from '@tanstack/react-query';

import type {
  IndexSetProfileChangeResult,
  ProfileChangeResponse,
  ProfileChangeResponseJson,
} from 'components/indices/IndexSetFieldTypes/types';
import { KEY_PREFIX as INDEX_SETS_OVERVIEW_KEY_PREFIX } from 'components/indices/IndexSetsOverview/fetchIndexSets';

export const parseProfileChangeResponse = (response: ProfileChangeResponseJson): ProfileChangeResponse =>
  Object.fromEntries(
    Object.entries(response).map(([indexSetId, { successfully_performed, failures, errors }]) => [
      indexSetId,
      {
        indexSetId,
        applied: successfully_performed > 0,
        failures: failures.map(({ failure_explanation }) => failure_explanation),
        errors,
      },
    ]),
  );

const hasProblems = ({ failures, errors }: IndexSetProfileChangeResult) => failures.length > 0 || errors.length > 0;

export const resultsWithProblems = (response: ProfileChangeResponse) => Object.values(response).filter(hasProblems);

export const appliedIndexSetIds = (response: ProfileChangeResponse) =>
  Object.values(response)
    .filter(({ applied }) => applied)
    .map(({ indexSetId }) => indexSetId);

export const refetchAfterProfileChange = (queryClient: QueryClient) =>
  Promise.all([
    queryClient.refetchQueries({ queryKey: ['indexSetFieldTypes'], type: 'active' }),
    queryClient.invalidateQueries({ queryKey: ['indexSet'] }),
    queryClient.invalidateQueries({ queryKey: INDEX_SETS_OVERVIEW_KEY_PREFIX }),
    queryClient.invalidateQueries({ queryKey: ['indexSetFieldTypeProfile'] }),
  ]);
