import React from 'react';
import { act } from 'react-dom/test-utils';
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
    fieldTypes = initialFieldTypes
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
