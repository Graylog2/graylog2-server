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

import asMock from 'helpers/mocking/AsMock';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';

import IndexerClusterHealthSummary from './IndexerClusterHealthSummary';

jest.mock('hooks/useCurrentUser');

const health = (status: string) => ({
  status,
  shards: { active: 113, initializing: 0, relocating: 0, unassigned: status === 'green' ? 0 : 2 },
});

describe('IndexerClusterHealthSummary', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('links an unhealthy cluster to the allocation explanation on the Indices page', async () => {
    render(<IndexerClusterHealthSummary health={health('yellow')} />);

    expect(await screen.findByRole('link', { name: 'What does this mean?' })).toHaveAttribute(
      'href',
      '/system/indices/management?allocation=explain',
    );
  });

  it('links a healthy cluster to the allocation explanation too', async () => {
    render(<IndexerClusterHealthSummary health={health('green')} />);

    expect(await screen.findByRole('link', { name: 'What does this mean?' })).toHaveAttribute(
      'href',
      '/system/indices/management?allocation=explain',
    );
  });

  it('links to the documentation for users who may not explain allocation', async () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(['indices:read']).build());
    render(<IndexerClusterHealthSummary health={health('red')} />);

    expect((await screen.findByRole('link', { name: /What does this mean\?/ })).getAttribute('href')).toMatch(
      /^https?:\/\//,
    );
  });
});
