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
import { MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';

import Widget from 'views/logic/widgets/Widget';
import TimeLocalizeContext from 'contexts/TimeLocalizeContext';
import { GlobalOverrideStore, GlobalOverrideStoreState } from 'views/stores/GlobalOverrideStore';
import GlobalOverride from 'views/logic/search/GlobalOverride';

import TimerangeInfo from './TimerangeInfo';

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', jest.fn()],
  ),
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({
      result: {
        results: {
          'active-query-id': {
            searchTypes: {
              'search-type-id': {
                effective_timerange: {
                  type: 'absolute', from: '2021-03-27T14:32:31.894Z', to: '2021-04-26T14:32:48.000Z',
                },
              },
            },
          },
        },
      },
      widgetMapping: {
        get: jest.fn(() => ({
          first: jest.fn(() => 'search-type-id'),
        })),
      },
    })],
  ),
}));

describe('TimerangeInfo', () => {
  const widget = Widget.empty();

  const SUT = (props) => (
    <TimeLocalizeContext.Provider value={{ localizeTime: (str) => str }}>
      <TimerangeInfo {...props} />
    </TimeLocalizeContext.Provider>
  );

  it('should display the effective timerange as title', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    render(<SUT widget={relativeWidget} activeQuery="active-query-id" widgetId="widget-id" />);

    expect(screen.getByTitle('2021-03-27T14:32:31.894Z - 2021-04-26T14:32:48.000Z')).toBeInTheDocument();
  });

  it('should display a relative timerange', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    render(<SUT widget={relativeWidget} />);

    expect(screen.getByText('an hour ago - Now')).toBeInTheDocument();
  });

  it('should display a relative timerange with from and to', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', from: 3000, to: 2000 }).build();
    render(<SUT widget={relativeWidget} />);

    expect(screen.getByText('an hour ago - 33 minutes ago')).toBeInTheDocument();
  });

  it('should display a All Time', () => {
    const relativeWidget = widget.toBuilder().timerange({ type: 'relative', range: 0 }).build();
    render(<SUT widget={relativeWidget} />);

    expect(screen.getByText('All Time')).toBeInTheDocument();
  });

  it('should display a absolute timerange', () => {
    const absoluteWidget = widget.toBuilder()
      .timerange({ type: 'absolute', from: '2021-03-27T14:32:31.894Z', to: '2021-04-26T14:32:48.000Z' })
      .build();
    render(<SUT widget={absoluteWidget} />);

    expect(screen.getByText('2021-03-27T14:32:31.894Z - 2021-04-26T14:32:48.000Z')).toBeInTheDocument();
  });

  it('should display a keyword timerange', () => {
    const keywordWidget = widget.toBuilder()
      .timerange({ type: 'keyword', keyword: '5 minutes ago' })
      .build();
    render(<SUT widget={keywordWidget} />);

    expect(screen.getByText('5 minutes ago')).toBeInTheDocument();
  });

  it('should display global override', () => {
    const state: GlobalOverrideStoreState = GlobalOverride.empty().toBuilder().timerange({ type: 'relative', range: 3000 }).build();
    asMock(GlobalOverrideStore.getInitialState).mockReturnValue(state);

    const keywordWidget = widget.toBuilder()
      .timerange({ type: 'keyword', keyword: '5 minutes ago' })
      .build();

    render(<SUT widget={keywordWidget} />);

    expect(screen.getByText('Global Override: an hour ago - Now')).toBeInTheDocument();
  });
});
