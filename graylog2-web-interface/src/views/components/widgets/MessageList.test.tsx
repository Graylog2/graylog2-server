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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import { TIMESTAMP_FIELD, Messages } from 'views/Constants';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useCurrentSearchTypesResults from 'views/components/widgets/useCurrentSearchTypesResults';
import useAppDispatch from 'stores/useAppDispatch';
import { finishedLoading } from 'views/logic/slices/searchExecutionSlice';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import SearchResult from 'views/logic/SearchResult';
import reexecuteSearchTypes from 'views/components/widgets/reexecuteSearchTypes';
import type { SearchErrorResponse } from 'views/logic/SearchError';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useAutoRefresh from 'views/hooks/useAutoRefresh';

import type { MessageListResult } from './MessageList';
import MessageList from './MessageList';
import type { TRenderCompletionCallback } from './RenderCompletionCallback';
import RenderCompletionCallback from './RenderCompletionCallback';

const mockEffectiveTimeRange: AbsoluteTimeRange = {
  from: '2019-11-15T14:40:48.666Z',
  to: '2019-11-29T14:40:48.666Z',
  type: 'absolute',
};

jest.mock('stores/inputs/InputsStore', () => ({
  InputsStore: MockStore(),
  InputsActions: { list: jest.fn(() => Promise.resolve()) },
}));

jest.mock('views/hooks/useAutoRefresh');

const searchTypeResults = {
  'search-type-id': {
    effectiveTimerange: mockEffectiveTimeRange,
    type: '',
    total: 0,
  },
};

const dummySearchJobResults = {
  errors: [],
  execution: { cancelled: false, completed_exceptionally: false, done: true },
  id: 'foo',
  owner: 'me',
  search_id: 'bar',
  results: {},
};
jest.mock('views/hooks/useActiveQueryId');
jest.mock('views/components/widgets/useCurrentSearchTypesResults');
jest.mock('views/components/widgets/reexecuteSearchTypes');
jest.mock('stores/useAppDispatch');

describe('MessageList', () => {
  const config = MessagesWidgetConfig.builder().fields([]).build();

  const data: MessageListResult = {
    id: 'search-type-id',
    type: 'messages',
    messages: [
      {
        highlight_ranges: {},
        index: 'graylog_42',
        message: {
          _id: 'deadbeef',
          file_name: 'frank.txt',
          timestamp: '2018-09-26T12:42:49.234Z',
        },
      },
    ],
    total: 1,
  };

  useViewsPlugin();

  beforeAll(() => {
    asMock(useAutoRefresh).mockReturnValue({
      refreshConfig: null,
      startAutoRefresh: () => {},
      stopAutoRefresh: () => {},
      restartAutoRefresh: () => {},
      animationId: 'animation-id',
    });
  });

  beforeEach(() => {
    asMock(useActiveQueryId).mockReturnValue('somequery');
    // @ts-expect-error
    asMock(useCurrentSearchTypesResults).mockReturnValue(searchTypeResults);
    asMock(InputsStore.getInitialState).mockReturnValue({ inputs: [] });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const findTable = () => screen.findByRole('table');

  const clickNextPageButton = () => {
    const paginationListItem = screen.getByRole('listitem', { name: /next/i });

    const nextPageButton = within(paginationListItem).getByRole('button');
    userEvent.click(nextPageButton);
  };

  const SimpleMessageList = ({ data: _data = data, config: _config = config, fields = Immutable.List([]), ...props }: Partial<React.ComponentProps<typeof MessageList>>) => (
    <TestStoreProvider>
      <MessageList title="Message List"
                   editing={false}
                   filter=""
                   type="messages"
                   id="message-list"
                   queryId="deadbeef"
                   toggleEdit={() => {}}
                   setLoadingState={() => {}}
                   data={_data}
                   config={_config}
                   fields={fields}
                   height={480}
                   width={640}
                   {...props} />
    </TestStoreProvider>
  );

  it('should render width widget fields', async () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];

    const configWithFields = MessagesWidgetConfig.builder().fields([TIMESTAMP_FIELD, 'file_name']).build();

    render(
      <SimpleMessageList config={configWithFields}
                         fields={Immutable.List(fields)} />,
    );

    await screen.findByText('file_name');
    await screen.findByText(TIMESTAMP_FIELD);
  });

  it('should render when widget has no fields', async () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    const emptyConfig = MessagesWidgetConfig.builder().fields([]).build();

    render(
      <SimpleMessageList config={emptyConfig}
                         fields={Immutable.List(fields)} />,
    );

    await findTable();

    expect(screen.queryByText('file_name')).not.toBeInTheDocument();
  });

  // eslint-disable-next-line jest/expect-expect
  it('renders also when `inputs` is undefined', async () => {
    asMock(InputsStore.getInitialState).mockReturnValue({ inputs: undefined });

    render(<SimpleMessageList />);

    await findTable();
  });

  it('refreshs Inputs list upon mount', () => {
    render(<SimpleMessageList />);

    expect(InputsActions.list).toHaveBeenCalled();
  });

  it('reexecute query for search type, when using pagination', async () => {
    const dispatch = jest.fn().mockResolvedValue(finishedLoading({
      result: new SearchResult(dummySearchJobResults),
      widgetMapping: Immutable.Map(),
    }));
    asMock(useAppDispatch).mockReturnValue(dispatch);
    const searchTypePayload = { [data.id]: { limit: Messages.DEFAULT_LIMIT, offset: Messages.DEFAULT_LIMIT } };
    const secondPageSize = 10;

    render(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }} />);

    clickNextPageButton();

    await waitFor(() => expect(reexecuteSearchTypes).toHaveBeenCalledWith(searchTypePayload, mockEffectiveTimeRange));
  });

  it('disables refresh actions, when using pagination', async () => {
    const stopAutoRefresh = jest.fn();

    asMock(useAutoRefresh).mockReturnValue({
      refreshConfig: null,
      startAutoRefresh: () => {},
      stopAutoRefresh,
      restartAutoRefresh: () => {},
      animationId: 'animation-id',
    });

    const dispatch = jest.fn().mockResolvedValue(finishedLoading({
      result: new SearchResult(dummySearchJobResults),
      widgetMapping: Immutable.Map(),
    }));
    asMock(useAppDispatch).mockReturnValue(dispatch);
    const secondPageSize = 10;

    render(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }} />);

    clickNextPageButton();

    await waitFor(() => expect(stopAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('displays error description, when using pagination throws an error', async () => {
    const dispatch = jest.fn().mockResolvedValue(finishedLoading({
      result: new SearchResult({
        ...dummySearchJobResults,
        errors: [{
          description: 'Error description',
        } as SearchErrorResponse],
      }),
      widgetMapping: Immutable.Map(),
    }));
    asMock(useAppDispatch).mockReturnValue(dispatch);

    const secondPageSize = 10;

    render(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }} />);

    clickNextPageButton();

    await screen.findByText('Error description');
  });

  it('calls render completion callback after first render', async () => {
    const onRenderComplete: TRenderCompletionCallback = jest.fn();

    render(
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <SimpleMessageList />
      </RenderCompletionCallback.Provider>,
    );

    await waitFor(() => expect(onRenderComplete).toHaveBeenCalled());
  });
});
