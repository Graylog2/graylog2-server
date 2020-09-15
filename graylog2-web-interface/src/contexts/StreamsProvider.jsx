// @flow strict
import * as React from 'react';
import { useEffect } from 'react';

import connect from 'stores/connect';
import { StreamsActions, StreamsStore } from 'views/stores/StreamsStore';

import StreamsContext from './StreamsContext';

type Props = {
  children: React.Node,
  streams: ?Array<*>,
};

const StreamsProvider = ({ children, streams }: Props) => {
  useEffect(() => {
    StreamsActions.refresh();
  }, []);

  return (
    <StreamsContext.Provider value={streams}>
      {children}
    </StreamsContext.Provider>
  );
};

export default connect(StreamsProvider, { streams: StreamsStore }, ({ streams: { streams } = {} }) => ({ streams }));
