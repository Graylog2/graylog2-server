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

import useSearchResult from 'views/hooks/useSearchResult';
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import { TIMESTAMP_FIELD, Messages } from 'views/Constants';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useCurrentSearchTypesResults from 'views/components/widgets/useCurrentSearchTypesResults';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { finishedLoading } from 'views/logic/slices/searchExecutionSlice';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import SearchResult from 'views/logic/SearchResult';
import reexecuteSearchTypes from 'views/components/widgets/reexecuteSearchTypes';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useMessageListPluggableBulkActions from 'views/components/widgets/useMessageListPluggableBulkActions';
import useSelectedMessageEntities from 'views/hooks/useSelectedMessageEntities';

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
jest.mock('views/hooks/useSearchResult');

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
  results: {
    'deadbeef': {
      query: { search_types: [{ id: 'search-type-id', limit: 10000 }] },
      execution_stats: {},
      errors: [],
    },
  },
};
jest.mock('views/hooks/useActiveQueryId');
jest.mock('views/components/widgets/useCurrentSearchTypesResults');
jest.mock('views/components/widgets/reexecuteSearchTypes');
jest.mock('views/stores/useViewsDispatch');
jest.mock('views/components/widgets/useMessageListPluggableBulkActions');

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
    asMock(useMessageListPluggableBulkActions).mockReturnValue({
      pluggableBulkActions: null,
      pluggableBulkActionModals: null,
    });
  });

  afterEach(() => {
    asMock(useSearchResult).mockReset();
  });

  const findTable = () => screen.findByRole('table');

  const clickNextPageButton = async () => {
    const paginationListItem = screen.getByRole('listitem', { name: /next/i });

    const nextPageButton = within(paginationListItem).getByRole('button');
    await userEvent.click(nextPageButton);
  };

  const openBulkActionsMenu = async () => {
    const bulkActionsButton = await screen.findByRole('button', { name: /bulk actions/i });

    await userEvent.click(bulkActionsButton);
  };

  const SimpleMessageList = ({
    data: _data = data,
    config: _config = config,
    fields = Immutable.List([]),
    ...props
  }: Partial<React.ComponentProps<typeof MessageList>>) => (
    <TestStoreProvider>
      <MessageList
        title="Message List"
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
        {...props}
      />
    </TestStoreProvider>
  );

  it('should render width widget fields', async () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];

    const configWithFields = MessagesWidgetConfig.builder().fields([TIMESTAMP_FIELD, 'file_name']).build();

    render(<SimpleMessageList config={configWithFields} fields={Immutable.List(fields)} />);

    await screen.findByText('file_name');
    await screen.findByText(TIMESTAMP_FIELD);
  });

  it('should render when widget has no fields', async () => {
    const fields = [new FieldTypeMapping('file_name', new FieldType('string', ['full-text-search'], []))];
    const emptyConfig = MessagesWidgetConfig.builder().fields([]).build();

    render(<SimpleMessageList config={emptyConfig} fields={Immutable.List(fields)} />);

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
    const dispatch = jest.fn().mockResolvedValue(
      finishedLoading({
        result: new SearchResult(dummySearchJobResults),
      }),
    );
    asMock(useViewsDispatch).mockReturnValue(dispatch);
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

    const dispatch = jest.fn().mockResolvedValue(
      finishedLoading({
        result: new SearchResult(dummySearchJobResults),
      }),
    );
    asMock(useViewsDispatch).mockReturnValue(dispatch);
    const secondPageSize = 10;

    render(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + secondPageSize }} />);

    clickNextPageButton();

    await waitFor(() => expect(stopAutoRefresh).toHaveBeenCalledTimes(1));
  });

  it('displays search result limit errors', async () => {
    asMock(useSearchResult).mockReturnValue({
      result: new SearchResult({
        ...dummySearchJobResults,
        errors: [
          {
            query_id: 'deadbeef',
            search_type_id: 'search-type-id',
            description: 'Error description',
            backtrace: undefined,
            type: 'result_window_limit',
            result_window_limit: 10000,
          },
        ],
      }),
    });

    render(<SimpleMessageList />);

    await screen.findByText(
      'Elasticsearch limits the search result to 10000 messages. With a page size of 10000 messages, you can use the first 1 pages. Error description',
    );
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

  it('does not render bulk selection checkboxes when bulk actions are not defined', async () => {
    render(<SimpleMessageList />);

    await findTable();

    expect(screen.queryByRole('checkbox', { name: /select message/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /select all visible messages/i })).not.toBeInTheDocument();
  });

  it('bulk actions and row checkboxes update selected message state', async () => {
    const BulkActions = () => {
      const { setSelectedEntities } = useSelectedEntities();

      return (
        <button onClick={() => setSelectedEntities([])} type="button">
          Reset selection
        </button>
      );
    };

    asMock(useMessageListPluggableBulkActions).mockReturnValue({
      pluggableBulkActions: <BulkActions />,
      pluggableBulkActionModals: null,
    });

    render(<SimpleMessageList />);

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select message/i });
    await userEvent.click(rowCheckboxes[0]);

    await screen.findByText('1 item selected');
    await openBulkActionsMenu();
    const customBulkAction = await screen.findByRole('button', { name: /reset selection/i });

    await userEvent.click(customBulkAction);

    await waitFor(() => expect(screen.queryByText('1 item selected')).not.toBeInTheDocument());
    await waitFor(() => expect(rowCheckboxes[0]).not.toBeChecked());
  });

  it('selects and deselects all visible messages', async () => {
    const secondMessageId = 'feedface';

    asMock(useMessageListPluggableBulkActions).mockReturnValue({
      pluggableBulkActions: <div>Example bulk action</div>,
      pluggableBulkActionModals: null,
    });

    render(
      <SimpleMessageList
        data={{
          ...data,
          messages: [
            ...data.messages,
            {
              highlight_ranges: {},
              index: 'graylog_43',
              message: {
                _id: secondMessageId,
                file_name: 'pam.txt',
                timestamp: '2018-09-26T12:43:49.234Z',
              },
            },
          ],
          total: 2,
        }}
      />,
    );

    const rowCheckboxes = await screen.findAllByRole('checkbox', { name: /select message/i });

    expect(rowCheckboxes[0]).not.toBeChecked();
    expect(rowCheckboxes[1]).not.toBeChecked();

    const selectAllCheckbox = await screen.findByRole('checkbox', { name: /select all visible messages/i });
    await userEvent.click(selectAllCheckbox);

    await waitFor(() => expect(rowCheckboxes[0]).toBeChecked());
    await waitFor(() => expect(rowCheckboxes[1]).toBeChecked());
    await screen.findByText(/2 items selected/i);

    const deselectAllCheckbox = await screen.findByRole('checkbox', { name: /deselect all visible messages/i });
    await userEvent.click(deselectAllCheckbox);

    await waitFor(() => expect(rowCheckboxes[0]).not.toBeChecked());
    await waitFor(() => expect(rowCheckboxes[1]).not.toBeChecked());
  });

  it('supports a complete bulk selection setup as example usage', async () => {
    const BulkActions = () => {
      const { selectedEntities, setSelectedEntities } = useSelectedEntities();

      return (
        <button onClick={() => setSelectedEntities([])} type="button">
          Clear {selectedEntities.length}
        </button>
      );
    };

    asMock(useMessageListPluggableBulkActions).mockReturnValue({
      pluggableBulkActions: <BulkActions />,
      pluggableBulkActionModals: null,
    });

    render(<SimpleMessageList />);

    const rowCheckbox = await screen.findByRole('checkbox', { name: /select message/i });
    await userEvent.click(rowCheckbox);

    await screen.findByText(/1 item selected/i);
    expect(await screen.findByRole('checkbox', { name: /deselect message/i })).toBeChecked();

    await openBulkActionsMenu();
    const clearButton = await screen.findByRole('button', { name: /clear 1/i });
    await userEvent.click(clearButton);

    await waitFor(() => expect(screen.queryByText(/1 item selected/i)).not.toBeInTheDocument());
    expect(await screen.findByRole('checkbox', { name: /select message/i })).not.toBeChecked();
  });

  it('preserves selected message data across pages for bulk actions', async () => {
    const dispatch = jest.fn().mockResolvedValue(
      finishedLoading({
        result: new SearchResult(dummySearchJobResults),
      }),
    );
    asMock(useViewsDispatch).mockReturnValue(dispatch);

    const BulkActions = () => {
      const { selectedEntities } = useSelectedMessageEntities();

      return <span>{selectedEntities.map(({ index }) => index).join(',')}</span>;
    };

    asMock(useMessageListPluggableBulkActions).mockReturnValue({
      pluggableBulkActions: <BulkActions />,
      pluggableBulkActionModals: null,
    });

    render(<SimpleMessageList data={{ ...data, total: Messages.DEFAULT_LIMIT + 1 }} />);

    const rowCheckbox = await screen.findByRole('checkbox', { name: /select message/i });
    await userEvent.click(rowCheckbox);

    await screen.findByText('1 item selected');
    await openBulkActionsMenu();
    await screen.findByText('graylog_42');

    await clickNextPageButton();

    await waitFor(() => expect(reexecuteSearchTypes).toHaveBeenCalled());
    await openBulkActionsMenu();
    await screen.findByText('graylog_42');
  });
});
