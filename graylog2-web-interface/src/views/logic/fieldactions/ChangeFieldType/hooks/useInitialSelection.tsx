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
import { useEffect, useMemo } from 'react';

import { useStore } from 'stores/connect';
import type { Stream } from 'views/stores/StreamsStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import useCurrentStream from 'views/logic/fieldactions/ChangeFieldType/hooks/useCurrentStream';
import type { IndexSet, IndexSetsStoreState } from 'stores/indices/IndexSetsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';

const streamsMapper = ({ streams }) => streams.map((stream: Stream) => ({ indexSet: stream.index_set_id, id: stream.id }));

const indexSetsStoreMapper = ({ indexSets }: IndexSetsStoreState): Record<string, IndexSet> => {
  if (!indexSets) return null;

  return Object.fromEntries(indexSets.map((indexSet) => ([indexSet.id, indexSet])));
};

const useInitialSelection = () => {
  const currentStreams = useCurrentStream();
  const indexSets = useStore(IndexSetsStore, indexSetsStoreMapper);
  const availableStreams: Array<{ indexSet: string, id: string }> = useStore(StreamsStore, streamsMapper);

  return useMemo(() => {
    const currentStreamSet = new Set(currentStreams);
    const filterFn = currentStreamSet.size > 0 ? ({ id, indexSet }) => currentStreamSet.has(id) && isIndexFieldTypeChangeAllowed(indexSets[indexSet]) : () => true;

    return indexSets ? availableStreams.filter(filterFn).map(({ indexSet }) => indexSet) : [];
  }, [availableStreams, currentStreams, indexSets]);
};

export default useInitialSelection;
