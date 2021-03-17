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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import { SearchActions } from 'views/stores/SearchStore';
import SearchButton from 'views/components/searchbar/SearchButton';

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    refresh: jest.fn(),
  },
}));

describe('SearchButton', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should perform refresh when not dirty', () => {
    render(<SearchButton />);

    const button = screen.getByTitle('Perform search');

    fireEvent.click(button);

    expect(SearchActions.refresh).toHaveBeenCalledTimes(1);
  });

  it('should not perform refresh when not dirty and disabled', () => {
    render(<SearchButton disabled />);

    const button = screen.getByTitle('Perform search');

    fireEvent.click(button);

    expect(SearchActions.refresh).not.toHaveBeenCalled();
  });
});
