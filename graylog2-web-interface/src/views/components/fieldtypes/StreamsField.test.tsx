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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import StreamsContext from 'contexts/StreamsContext';
import type { Stream } from 'logic/streams/types';

import StreamsField from './StreamsField';

const streams = [{ id: 'stream-id-1', title: 'Stream One' }] as Array<Stream>;

describe('StreamsField', () => {
  it('shows stream titles for stream ids', async () => {
    render(
      <StreamsContext.Provider value={streams}>
        <StreamsField value={['stream-id-1', 'stream-id-2']} />
      </StreamsContext.Provider>,
    );

    await screen.findByText('Stream One');
    await screen.findByText('stream-id-2');
  });

  it('shows stream ids while streams are not loaded yet', async () => {
    render(
      <StreamsContext.Provider value={undefined}>
        <StreamsField value="stream-id-1" />
      </StreamsContext.Provider>,
    );

    await screen.findByText('stream-id-1');
  });
});
