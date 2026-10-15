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

import StreamTitle from './StreamTitle';

const stream = { id: '5f3a1b2c3d4e5f6a7b8c9d0e', title: 'Firewall Logs' } as Stream;

const SUT = ({ id }: { id: string }) => (
  <StreamsContext.Provider value={[stream]}>
    <StreamTitle id={id}>Fallback</StreamTitle>
  </StreamsContext.Provider>
);

describe('StreamTitle', () => {
  it('renders title of known stream', async () => {
    render(<SUT id={stream.id} />);

    await screen.findByText('Firewall Logs');
  });

  it('renders fallback for unknown stream', async () => {
    render(<SUT id="000000000000000000000000" />);

    await screen.findByText('Fallback');
  });
});
