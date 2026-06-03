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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { Extractors } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';

import ExtractorsSection from './ExtractorsSection';

jest.mock('@graylog/server-api', () => ({
  Extractors: {
    list: jest.fn(),
  },
}));

type ExtractorsListResponse = Awaited<ReturnType<typeof Extractors.list>>;

const input = {
  id: 'input-1',
  title: 'My Test Input',
  type: 'org.graylog2.inputs.raw.tcp.RawTCPInput',
  name: 'Raw/Plaintext TCP',
  global: true,
  node: '',
  created_at: '2024-01-01T00:00:00Z',
  creator_user_id: 'admin',
  attributes: {},
  static_fields: {},
  content_pack: '',
};

describe('ExtractorsSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('lazy-loads extractors from the per-input endpoint', async () => {
    asMock(Extractors.list).mockResolvedValue({
      total: 2,
      extractors: [
        { id: 'e1', title: 'Parse JSON', type: 'json', source_field: 'message', target_field: 'parsed' },
        { id: 'e2', title: 'Grok message', type: 'grok', source_field: 'message', target_field: 'fields' },
      ],
    } as ExtractorsListResponse);

    render(<ExtractorsSection input={input} />);

    await waitFor(() => expect(Extractors.list).toHaveBeenCalledWith('input-1'));

    expect(await screen.findByText('Parse JSON')).toBeInTheDocument();
    expect(screen.getByText('Grok message')).toBeInTheDocument();
  });

  it('shows an empty-state when the input has no configured extractors', async () => {
    asMock(Extractors.list).mockResolvedValue({ total: 0, extractors: [] } as ExtractorsListResponse);

    render(<ExtractorsSection input={input} />);

    expect(await screen.findByText(/has no configured extractors/i)).toBeInTheDocument();
  });
});
