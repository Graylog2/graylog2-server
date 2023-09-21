import { useMemo } from 'react';

import { useStore } from 'stores/connect';
import type { Stream } from 'views/stores/StreamsStore';
import { StreamsStore } from 'views/stores/StreamsStore';

const streamsMapper = ({ streams }) => streams.map((stream: Stream) => ({ indexSet: stream.index_set_id, id: stream.id }));

const useInitialSelection = (currentStreams: Array<string>) => {
  const availableStreams: Array<{ indexSet: string, id: string }> = useStore(StreamsStore, streamsMapper);

  return useMemo(() => {
    const currentStreamSet = new Set(currentStreams);
    const filterFn = currentStreamSet.size > 0 ? ({ id }) => currentStreamSet.has(id) : () => true;

    return availableStreams.filter(filterFn).map(({ indexSet }) => indexSet);
  }, [availableStreams, currentStreams]);
};

export default useInitialSelection;
