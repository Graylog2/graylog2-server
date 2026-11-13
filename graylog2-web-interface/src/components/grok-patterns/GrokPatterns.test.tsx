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
import React from 'react';
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';
import { loadGrokPatterns, searchGrokPatternsPaginated } from 'hooks/useGrokPatterns';

import GrokPatterns from './GrokPatterns';

jest.mock('hooks/useCurrentUser');

jest.mock('hooks/useGrokPatterns', () => ({
  ...jest.requireActual('hooks/useGrokPatterns'),
  loadGrokPatterns: jest.fn(),
  searchGrokPatternsPaginated: jest.fn(),
}));

const ipPattern = { id: 'ip-id', name: 'IP', pattern: '(?:%{IPV6}|%{IPV4})', content_pack: null };
const intPattern = { id: 'int-id', name: 'INT', pattern: '(?:[+-]?(?:[0-9]+))', content_pack: null };

describe('<GrokPatterns />', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);

    asMock(searchGrokPatternsPaginated).mockResolvedValue({
      patterns: [ipPattern],
      pagination: { count: 1, total: 1, page: 1, perPage: 10, query: 'IP' },
    });

    asMock(loadGrokPatterns).mockImplementation((callback) => {
      callback([intPattern, ipPattern]);

      return Promise.resolve({ patterns: [intPattern, ipPattern] });
    });
  });

  it('offers all patterns in the create modal, independent of the list search', async () => {
    render(<GrokPatterns />);

    await screen.findByRole('cell', { name: 'IP' });

    await userEvent.click(await screen.findByRole('button', { name: /create pattern/i }));

    const modal = await screen.findByRole('dialog');
    await userEvent.type(within(modal).getByRole('textbox', { name: /filter pattern/i }), 'INT');

    await within(modal).findByText('INT');
  });
});
