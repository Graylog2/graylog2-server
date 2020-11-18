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
// @flow strict
import * as React from 'react';
import { useEffect } from 'react';

import connect from 'stores/connect';
import { StreamsActions, StreamsStore } from 'views/stores/StreamsStore';

import StreamsContext from './StreamsContext';

type Props = {
  children: React.ReactElement,
  streams: Array<any> | undefined | null,
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

export default connect(StreamsProvider, { streams: StreamsStore }, ({ streams: { streams } = { streams: undefined } }) => ({ streams }));
