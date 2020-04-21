import PropTypes from 'prop-types';
import React from 'react';
import { act } from 'react-dom/test-utils';

import { simpleFields, simpleQueryFields } from 'fixtures/fields';
import { render, cleanup } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';

import SearchResult from 'views/components/SearchResult';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    execute: jest.fn(() => Promise.resolve()),
  },
  SearchStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({
      result: {
        forId: jest.fn(() => ({})),
      },
      widgetMapping: {},
    }),
  },
}));
jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: {
    getInitialState: () => ({
      activeQuery: 'aQueryId',
      id: 'aViewId',
    }),
    listen: () => jest.fn(),
  },
}));
jest.mock('views/stores/SearchLoadingStateStore', () => ({
  SearchLoadingStateStore: {
    getInitialState: jest.fn(() => ({ isLoading: false })),
    listen: () => jest.fn(),
  },
}));

describe('SearchResult', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  const fieldTypesStroreState = { all: simpleFields(), queryFields: simpleQueryFields('aQueryId') };
  const SimpleSearchResult = ({ fieldTypes }) => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <SearchResult />
    </FieldTypesContext.Provider>
  );
  SimpleSearchResult.propTypes = {
    fieldTypes: PropTypes.object,
  };
  SimpleSearchResult.defaultProps = {
    fieldTypes: fieldTypesStroreState,
  };

  it('should show spinner with undefined fields', () => {
    const { getByText } = render(<SearchResult />);
    act(() => jest.advanceTimersByTime(200));
    expect(getByText('Loading...')).not.toBeNull();
  });
  it('should display loading indicator, when search is loading ', () => {
    asMock(SearchLoadingStateStore.getInitialState).mockImplementation(() => ({ isLoading: true }));
    const { getByText } = render(<SimpleSearchResult />);
    act(() => jest.advanceTimersByTime(500));
    expect(getByText('Updating search results...')).not.toBeNull();
  });
  it('should hide loading indicator, when search is not loading', () => {
    asMock(SearchLoadingStateStore.getInitialState).mockReturnValueOnce({ isLoading: false });
    const { queryByText } = render(<SimpleSearchResult />);
    expect(queryByText('Updating search results...')).toBeNull();
  });
  it('should display info message when field types and search results exists, but no widgets are defined', () => {
    const { getByText } = render(<SimpleSearchResult />);
    expect(getByText('Create a new widget by selecting a widget type in the left sidebar section "Create".')).not.toBeNull();
  });
});
