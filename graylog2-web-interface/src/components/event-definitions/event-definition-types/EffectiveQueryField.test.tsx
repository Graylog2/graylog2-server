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
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { SearchFilter } from 'components/event-definitions/event-definitions-types';

import EffectiveQueryField from './EffectiveQueryField';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

const URL = '/plugins/org.graylog.plugins.searchfilters/search_filters/effective_query';

describe('EffectiveQueryField', () => {
  beforeEach(() => {
    asMock(fetch).mockResolvedValue({ effective_query: '(action:login) AND (source:firewall)' });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const filters: SearchFilter[] = [
    {
      id: 'filter-1',
      type: 'inlineQueryString',
      title: 'Firewall',
      queryString: 'source:firewall',
      disabled: false,
      negation: false,
    },
  ];

  it('renders the effective query returned by the backend', async () => {
    render(<EffectiveQueryField queryString="action:login" filters={filters} />);

    await screen.findByText('(action:login) AND (source:firewall)');
  });

  it('posts the query and filters to the effective-query endpoint', async () => {
    render(<EffectiveQueryField queryString="action:login" filters={filters} />);

    await screen.findByText('(action:login) AND (source:firewall)');

    expect(fetch).toHaveBeenCalledWith('POST', qualifyUrl(URL), {
      query_string: 'action:login',
      filters: [
        {
          id: 'filter-1',
          type: 'inlineQueryString',
          title: 'Firewall',
          queryString: 'source:firewall',
          disabled: false,
          negation: false,
        },
      ],
    });
  });

  it('shows a fallback message when the request fails', async () => {
    asMock(fetch).mockRejectedValue(new Error('boom'));

    render(<EffectiveQueryField queryString="action:login" filters={filters} />);

    await screen.findByText('(failed to render effective query)');
  });
});
