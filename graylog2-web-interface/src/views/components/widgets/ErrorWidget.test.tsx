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
import { render, screen } from 'wrappedTestingLibrary';

import SearchError from 'views/logic/SearchError';

import ErrorWidget from './ErrorWidget';

describe('<ErrorWidget />', () => {
  it('should display a list item for every provided error', async () => {
    const errors = [
      new SearchError({
        description: 'The first error',
        query_id: 'query-id-1',
        search_type_id: 'search_type_id-1',
        type: 'query',
        backtrace: '',
      }),
      new SearchError({
        description: 'The second error',
        query_id: 'query-id-2',
        search_type_id: 'search_type_id-2',
        type: 'query',
        backtrace: '',
      }),
    ];

    render(<ErrorWidget errors={errors} />);

    await screen.findByText(errors[0].description);
    await screen.findByText(errors[1].description);
  });
});
