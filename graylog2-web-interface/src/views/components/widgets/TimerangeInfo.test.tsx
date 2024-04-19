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
import Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';

import SearchResult from 'views/logic/SearchResult';
import asMock from 'helpers/mocking/AsMock';
import Widget from 'views/logic/widgets/Widget';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { ALL_MESSAGES_TIMERANGE } from 'views/Constants';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useSearchResult from 'views/hooks/useSearchResult';
import type { SearchExecutionResult } from 'views/types';
import useGlobalOverride from 'views/hooks/useGlobalOverride';

import OriginalTimerangeInfo from './TimerangeInfo';

jest.mock('views/hooks/useGlobalOverride');

const defaultSearchResult = {
  execution: {
    cancelled: false,
    completed_exceptionally: false,
    done: true,
  },
  id: 'foo',
  owner: 'paul',
  search_id: 'deadbeef',
  errors: [],
};
const mockSearchStoreState = (storeState: Partial<SearchExecutionResult> = {}): SearchExecutionResult => ({
  result: new SearchResult({
    ...defaultSearchResult,
    results: {
      'active-query-id': {
        execution_stats: {},
        errors: [],
        search_types: {
          'search-type-id': {
            type: 'pivot',
            effective_timerange: {
              type: 'absolute', from: '2021-04-26T12:32:48.000Z', to: '2021-04-26T14:32:48.000Z',
            },
          },
        },
      },
    },
  }),
  widgetMapping: Immutable.Map({ 'widget-id': Immutable.Set(['search-type-id']) }),
  ...storeState,
});

jest.mock('views/hooks/useSearchResult');

const TimerangeInfo = (props: React.ComponentProps<typeof OriginalTimerangeInfo>) => (
  <TestStoreProvider>
    <OriginalTimerangeInfo {...props} />
  </TestStoreProvider>
);

describe('TimerangeInfo', () => {
  const widget = Widget.empty();

  useViewsPlugin();

  beforeEach(() => {
    asMock(useSearchResult).mockReturnValue(mockSearchStoreState());
    asMock(useGlobalOverride).mockReturnValue(GlobalOverride.empty());
  });

  it('should display the effective timerange as title', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    render(<TimerangeInfo widget={relativeWidget} activeQuery="active-query-id" widgetId="widget-id" />);

    expect(screen.getByTitle('2021-04-26T14:32:48.000+02:00 - 2021-04-26T16:32:48.000+02:00')).toBeInTheDocument();
  });

  it('should display a relative timerange', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    render(<TimerangeInfo widget={relativeWidget} />);

    expect(screen.getByText('50 minutes ago - Now')).toBeInTheDocument();
  });

  it('should display a relative timerange with from and to', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', from: 3000, to: 2000 }).build();
    render(<TimerangeInfo widget={relativeWidget} />);

    expect(screen.getByText('50 minutes ago - 33 minutes 20 seconds ago')).toBeInTheDocument();
  });

  it('should display a All Time', () => {
    const relativeWidget = widget.toBuilder().timerange(ALL_MESSAGES_TIMERANGE).build();
    render(<TimerangeInfo widget={relativeWidget} />);

    expect(screen.getByText('All Time')).toBeInTheDocument();
  });

  it('should display an absolute timerange', () => {
    const absoluteWidget = widget.toBuilder()
      .timerange({ type: 'absolute', from: '2021-03-27T14:32:31.894Z', to: '2021-04-26T14:32:48.000Z' })
      .build();
    render(<TimerangeInfo widget={absoluteWidget} />);

    expect(screen.getByText('2021-03-27 15:32:31.894 - 2021-04-26 16:32:48.000')).toBeInTheDocument();
  });

  it('should display a keyword timerange', () => {
    const keywordWidget = widget.toBuilder()
      .timerange({ type: 'keyword', keyword: '5 minutes ago' })
      .build();
    render(<TimerangeInfo widget={keywordWidget} />);

    expect(screen.getByText('5 minutes ago')).toBeInTheDocument();
  });

  it('should display global override', () => {
    const state = GlobalOverride.empty().toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    asMock(useGlobalOverride).mockReturnValue(state);

    const keywordWidget = widget.toBuilder()
      .timerange({ type: 'keyword', keyword: '5 minutes ago' })
      .build();

    render(<TimerangeInfo widget={keywordWidget} />);

    expect(screen.getByText('Global Override: 50 minutes ago - Now')).toBeInTheDocument();
  });

  it('should not throw error when related search type is empty', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 3000 }).build();

    asMock(useSearchResult).mockReturnValue(mockSearchStoreState({
      result: new SearchResult({
        ...defaultSearchResult,
        results: {
          'active-query-id': {
            execution_stats: {},
            errors: [],
            search_types: {},
          },
        },
      }),
    }) as SearchExecutionResult);

    render(<TimerangeInfo widget={relativeWidget} activeQuery="active-query-id" widgetId="widget-id" />);

    expect(screen.getByText('50 minutes ago - Now')).toBeInTheDocument();
  });

  it('should not throw error and display default time range when widget id does not exist in search widget mapping', () => {
    asMock(useSearchResult).mockReturnValue(mockSearchStoreState({
      widgetMapping: Immutable.Map(),
    }) as SearchExecutionResult);

    render(<TimerangeInfo widget={widget} activeQuery="active-query-id" widgetId="widget-id" />);

    expect(screen.getByText('5 minutes ago - Now')).toBeInTheDocument();
  });
});
