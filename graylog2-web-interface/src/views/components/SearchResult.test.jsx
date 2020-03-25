import React from 'react';
import { act } from 'react-dom/test-utils';

import { render, cleanup } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';

import SearchResult from 'views/components/SearchResult';

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesActions: {},
  FieldTypesStore: {
    listen: () => jest.fn(),
    getInitialState: jest.fn(() => ({
      all: {},
      queryFields: {
        get: jest.fn(() => {
          return {};
        }),
      },
    })),
  },
}));
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
  beforeEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('should show spinner while loading field types', () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValueOnce(undefined);
    const { getByText } = render(<SearchResult />);
    act(() => jest.advanceTimersByTime(200));
    expect(getByText('Loading...')).not.toBeNull();
  });
  it('should show spinner when there are no search results', () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValueOnce(undefined);
    const { getByText } = render(<SearchResult />);
    act(() => jest.advanceTimersByTime(200));
    expect(getByText('Loading...')).not.toBeNull();
  });
  it('should display loading indicator, when search is loading ', () => {
    asMock(SearchLoadingStateStore.getInitialState).mockImplementation(() => ({ isLoading: true }));
    const { getByText } = render(<SearchResult />);
    act(() => jest.advanceTimersByTime(500));
    expect(getByText('Updating search results...')).not.toBeNull();
  });
  it('should hide loading indicator, when search is not loading', () => {
    asMock(SearchLoadingStateStore.getInitialState).mockReturnValueOnce({ isLoading: false });
    const { queryByText } = render(<SearchResult />);
    expect(queryByText('Updating search results...')).toBeNull();
  });
  it('should display info message when field types and search results exists, but no widgets are defined', () => {
    const { getByText } = render(<SearchResult />);
    expect(getByText('Create a new widget by selecting a widget type in the left sidebar section "Create".')).not.toBeNull();
  });
});
