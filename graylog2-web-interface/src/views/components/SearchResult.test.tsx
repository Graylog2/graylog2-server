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
import { act } from 'react';
import { render } from 'wrappedTestingLibrary';

import { simpleFields, simpleQueryFields } from 'fixtures/fields';
import asMock from 'helpers/mocking/AsMock';
import SearchResult from 'views/components/SearchResult';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useIsLoading from 'views/hooks/useIsLoading';

jest.mock('views/hooks/useIsLoading');
jest.mock('views/components/Query', () => () => <span>Query Results</span>);

type SimpleSearchResultProps = {
  fieldTypes?: any;
};

describe('SearchResult', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const initialFieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('aQueryId') };
  const SimpleSearchResult = ({
    fieldTypes = initialFieldTypes,
  }: SimpleSearchResultProps) => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <SearchResult />
    </FieldTypesContext.Provider>
  );

  it('should show spinner with undefined fields', () => {
    const { getByText } = render(
      <SearchResult />,
    );

    act(() => { jest.advanceTimersByTime(200); });

    expect(getByText('Loading...')).not.toBeNull();
  });

  it('should display loading indicator, when search is loading', () => {
    asMock(useIsLoading).mockReturnValue(true);
    const { getByText } = render(<SimpleSearchResult />);

    act(() => { jest.advanceTimersByTime(500); });

    expect(getByText('Updating search results...')).not.toBeNull();
  });

  it('should hide loading indicator, when search is not loading', () => {
    asMock(useIsLoading).mockReturnValue(false);

    const { queryByText } = render(<SimpleSearchResult />);

    expect(queryByText('Updating search results...')).toBeNull();
  });

  it('renders query results', () => {
    const { getByText } = render(<SimpleSearchResult />);

    expect(getByText('Query Results')).not.toBeNull();
  });
});
