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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import StreamsContext from 'contexts/StreamsContext';

import CoreKeyMappers from './CoreKeyMappers';

const mapperFor = (type: string) => CoreKeyMappers.find((m) => m.type === type)!.useKeyMapper;

jest.mock('hooks/useInputs', () => ({
  useInputs: () => ({ 'input-1': { id: 'input-1', title: 'Syslog UDP' } }),
}));

jest.mock('hooks/useNodeSummaries', () => () => ({
  'node-1': { short_node_id: 'abc123', hostname: 'graylog-01' },
}));

describe('CoreKeyMappers', () => {
  it('resolves stream ids to titles and falls back to the raw id', () => {
    const wrapper = ({ children }: React.PropsWithChildren) => (
      <StreamsContext.Provider value={[{ id: 'stream-1', title: 'All messages' } as any]}>
        {children}
      </StreamsContext.Provider>
    );
    const { result } = renderHook(() => mapperFor('streams')([]), { wrapper });

    expect(result.current('stream-1')).toBe('All messages');
    expect(result.current('unknown')).toBe('unknown');
  });

  it('resolves input ids to titles', () => {
    const { result } = renderHook(() => mapperFor('input')([]));

    expect(result.current('input-1')).toBe('Syslog UDP');
    expect(result.current('missing')).toBe('missing');
  });

  it('formats node ids as "short_node_id / hostname"', () => {
    const { result } = renderHook(() => mapperFor('node')([]));

    expect(result.current('node-1')).toBe('abc123 / graylog-01');
    expect(result.current('missing')).toBe('missing');
  });
});
