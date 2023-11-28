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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { OrderedMap } from 'immutable';

import type { Attributes } from 'stores/PaginationTypes';
import { asMock } from 'helpers/mocking';
import useFilterValueSuggestions from 'components/common/EntityFilters/hooks/useFilterValueSuggestions';
import useFiltersWithTitle from 'components/common/EntityFilters/hooks/useFiltersWithTitle';

import EntityFilters from './EntityFilters';

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  .setSystemTime(mockedUnixTime);

jest.mock('logic/generateId', () => jest.fn(() => 'filter-id'));
jest.mock('components/common/EntityFilters/hooks/useFilterValueSuggestions');
jest.mock('components/common/EntityFilters/hooks/useFiltersWithTitle');

describe('<EntityFilters />', () => {
  const onChangeFiltersWithTitle = jest.fn();
  const attributes = [
    { id: 'title', title: 'Title', sortable: true },
    { id: 'description', title: 'Description', sortable: true },
    {
      id: 'disabled',
      title: 'Status',
      type: 'BOOLEAN',
      filterable: true,
      filter_options: [
        { value: 'true', title: 'Paused' },
        { value: 'false', title: 'Running' },
      ],
    },
    {
      id: 'type',
      title: 'Type',
      type: 'STRING',
      sortable: true,
      filterable: true,
      filter_options: [
        {
          value: 'string',
          title: 'String (aggregatable)',
        },
        {
          value: 'long',
          title: 'Number',
        },
      ],
    },
    {
      id: 'index_set_id',
      filterable: true,
      related_collection: 'index_sets',
      title: 'Index set',
      type: 'STRING',
    },
    {
      id: 'created_at',
      filterable: true,
      title: 'Created at',
      type: 'DATE',
    },
  ] as Attributes;

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useFiltersWithTitle).mockReturnValue({
      data: undefined,
      onChange: onChangeFiltersWithTitle,
      isInitialLoading: false,
    });
  });

  describe('boolean attribute', () => {
    it('should create filter', async () => {
      const setUrlQueryFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={undefined} />,
      );

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /status/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /running/i,
      }));

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        OrderedMap({
          disabled: [{
            title: 'Running',
            value: 'false',
          }],
        }),
        OrderedMap({ disabled: ['false'] }),
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ disabled: ['false'] })));
    });

    it('should update active filter on click', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({ disabled: [{ title: 'Running', value: 'false' }] }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
      );

      const activeFilter = await screen.findByTestId('disabled-filter-false');

      const toggleFilterButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });

      userEvent.click(toggleFilterButton);

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        OrderedMap({ disabled: [{ title: 'Paused', value: 'true' }] }),
        OrderedMap({ disabled: ['true'] }),
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ disabled: ['true'] })));
    });

    it('should prevent creating multiple filter for boolean value', async () => {
      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({ disabled: [{ title: 'Running', value: 'false' }] }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={() => {}}
                       urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
      );

      await screen.findByTestId('disabled-filter-false');

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      // eslint-disable-next-line testing-library/no-node-access
      expect(screen.getByRole('menuitem', { name: /status/i }).closest('li')).toHaveClass('disabled');
    });
  });

  describe('attribute with filter suggestions', () => {
    beforeEach(() => {
      asMock(useFilterValueSuggestions).mockReturnValue({
        data: {
          pagination: {
            total: 1,
          },
          suggestions: [
            { id: 'index-set-1', value: 'Default index set' },
            { id: 'index-set-2', value: 'Example index set' },
          ],
        },
        isInitialLoading: false,
      });
    });

    it('should create filter', async () => {
      const setUrlQueryFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes} setUrlQueryFilters={setUrlQueryFilters} urlQueryFilters={undefined} />,
      );

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /index set/i,
      }));

      userEvent.click(await screen.findByRole('button', {
        name: /default index set/i,
      }));

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        OrderedMap({ index_set_id: [{ title: 'Default index set', value: 'index-set-1' }] }),
        OrderedMap({ index_set_id: ['index-set-1'] }),
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ index_set_id: ['index-set-1'] })));
    });

    it('should update active filter', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({
          index_set_id: [
            { title: 'Default index set', value: 'index-set-1' },
          ],
        }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={OrderedMap({ index_set_id: ['index-set-1'] })} />,
      );

      const activeFilter = await screen.findByTestId('index_set_id-filter-index-set-1');

      const openSuggestionsButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });

      userEvent.click(openSuggestionsButton);

      userEvent.click(await screen.findByRole('button', {
        name: /example index set/i,
      }));

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        OrderedMap({ index_set_id: [{ title: 'Example index set', value: 'index-set-2' }] }),
        OrderedMap({ index_set_id: ['index-set-2'] }),
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ index_set_id: ['index-set-2'] })));
    });
  });

  describe('date attribute', () => {
    it('should create filter', async () => {
      const setUrlQueryFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={undefined} />,
      );

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /created at/i,
      }));

      const timeRangeForm = await screen.findByTestId('time-range-form');
      const submitButton = within(timeRangeForm).getByRole('button', {
        name: /create filter/i,
      });
      userEvent.click(submitButton);

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        OrderedMap({
          created_at: [{
            title: '2020-01-01 00:55:00.000 - Now',
            value: '2019-12-31T23:55:00.000+00:00><',
          }],
        }),
        OrderedMap({ created_at: ['2019-12-31T23:55:00.000+00:00><'] }),
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ created_at: ['2019-12-31T23:55:00.000+00:00><'] })));
    });

    it('should update active filter', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({
          created_at: [{
            title: '2020-01-01 00:55:00 - Now',
            value: '2019-12-31T23:55:00.001+00:00',
          }],
        }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={OrderedMap({ created_at: ['2019-12-31T23:55:00.000+00:00><'] })} />,
      );

      const activeFilter = await screen.findByTestId('created_at-filter-2019-12-31T23:55:00.001+00:00');

      const toggleFilterButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });
      userEvent.click(toggleFilterButton);

      userEvent.type(await screen.findByRole('textbox', { name: /from/i }), '{backspace}1');

      const timeRangeForm = await screen.findByTestId('time-range-form');
      const submitButton = within(timeRangeForm).getByRole('button', {
        name: /update filter/i,
      });
      userEvent.click(submitButton);

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        OrderedMap({
          created_at: [{
            title: '2020-01-01 00:55:00.001 - Now',
            value: '2019-12-31T23:55:00.001+00:00><',
          }],
        }),
        OrderedMap({ created_at: ['2019-12-31T23:55:00.001+00:00><'] }),
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ created_at: ['2019-12-31T23:55:00.001+00:00><'] })));
    });
  });

  describe('string attribute', () => {
    it('should prevent creating same filter multiple times', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({ type: [{ title: 'String', value: 'string' }] }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={OrderedMap({ type: ['string'] })} />,
      );

      await screen.findByTestId('type-filter-string');

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /type/i,
      }));

      // eslint-disable-next-line testing-library/no-node-access
      expect(screen.getByRole('menuitem', { name: /string/i }).closest('li')).toHaveClass('disabled');
    });
  });

  it('should display active filters', async () => {
    const setUrlQueryFilters = jest.fn();

    asMock(useFiltersWithTitle).mockReturnValue({
      data: OrderedMap({
        disabled: [{ title: 'Running', value: 'false' }],
      }),
      onChange: onChangeFiltersWithTitle,
      isInitialLoading: false,
    });

    render(
      <EntityFilters attributes={attributes}
                     setUrlQueryFilters={setUrlQueryFilters}
                     urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
    );

    await screen.findByTestId('disabled-filter-false');
  });

  it('should delete an active filter', async () => {
    const setUrlQueryFilters = jest.fn();

    asMock(useFiltersWithTitle).mockReturnValue({
      data: OrderedMap({
        disabled: [{ title: 'Running', value: 'false' }],
      }),
      onChange: onChangeFiltersWithTitle,
      isInitialLoading: false,
    });

    render(
      <EntityFilters attributes={attributes}
                     setUrlQueryFilters={setUrlQueryFilters}
                     urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
    );

    const activeFilter = await screen.findByTestId('disabled-filter-false');
    const deleteButton = within(activeFilter).getByRole('button', {
      name: /delete filter/i,
    });

    userEvent.click(deleteButton);

    await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(OrderedMap(), OrderedMap()));
    await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap()));
  });
});
