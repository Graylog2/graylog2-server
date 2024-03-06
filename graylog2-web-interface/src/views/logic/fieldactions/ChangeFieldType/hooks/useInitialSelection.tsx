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
import { useMemo } from 'react';

import { useStore } from 'stores/connect';
import type { Stream } from 'views/stores/StreamsStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import useCurrentStream from 'views/logic/fieldactions/ChangeFieldType/hooks/useCurrentStream';

const streamsMapper = ({ streams }) => streams.map((stream: Stream) => ({ indexSet: stream.index_set_id, id: stream.id }));

const useInitialSelection = () => {
  const currentStreams = useCurrentStream();
  const availableStreams: Array<{ indexSet: string, id: string }> = useStore(StreamsStore, streamsMapper);

  return useMemo(() => {
    const currentStreamSet = new Set(currentStreams);
    const filterFn = currentStreamSet.size > 0 ? ({ id }) => currentStreamSet.has(id) : () => true;

    return availableStreams.filter(filterFn).map(({ indexSet }) => indexSet);
  }, [availableStreams, currentStreams]);
};

export default useInitialSelection;
