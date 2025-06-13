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
import { render } from 'wrappedTestingLibrary';

import selectEvent from 'helpers/selectEvent';

import StreamsFilter from './StreamsFilter';

describe('StreamsFilter', () => {
  it('sorts stream names', async () => {
    const streams = [
      { key: 'First Stream', value: 'streamId1' },
      { key: 'Second Stream', value: 'streamId2' },
      { key: 'Yet another Stream', value: 'streamId3' },
      { key: '101 Stream', value: 'streamId4' },
    ];
    render(<StreamsFilter streams={streams} onChange={() => {}} />);

    await selectEvent.findOption('select streams', [
      '101 Stream',
      'Second Stream',
      'First Stream',
      'Yet another Stream',
    ]);
  });
});
