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

import type { Attributes } from 'stores/PaginationTypes';
import { asMock } from 'helpers/mocking';
import useFilterValueSuggestions from 'components/common/EntityFilters/hooks/useFilterValueSuggestions';
import useFiltersWithTitle from 'components/common/EntityFilters/hooks/useFiltersWithTitle';

import EntityFilters from './EntityFilters';

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  // @ts-expect-error
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
      filterable: true,
      id: 'index_set_id',
      related_collection: 'index_sets',
      title: 'Index set',
      type: 'STRING',
    },
    {
      filterable: true,
      id: 'created_at',
      title: 'Created at',
      type: 'DATE',
    },
  ] as Attributes;

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useFiltersWithTitle).mockReturnValue({
      data: undefined,
      onChange: onChangeFiltersWithTitle,
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
        {
          disabled: [{
            id: 'filter-id',
            title: 'Running',
            value: 'false',
          }],
        },
        { disabled: ['false'] },
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith({ disabled: ['false'] }));
    });

    it('should update active filter on click', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: { disabled: [{ id: 'filter-id', title: 'Running', value: 'false' }] },
        onChange: onChangeFiltersWithTitle,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={{ disabled: ['false'] }} />,
      );

      const activeFilter = await screen.findByTestId('filter-filter-id');

      const toggleFilterButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });

      userEvent.click(toggleFilterButton);

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        {
          disabled: [{ id: 'filter-id', title: 'Paused', value: 'true' }],
        },
        { disabled: ['true'] },
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith({ disabled: ['true'] }));
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
        { index_set_id: [{ id: 'filter-id', title: 'Default index set', value: 'index-set-1' }] },
        { index_set_id: ['index-set-1'] },
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith({ index_set_id: ['index-set-1'] }));
    });

    it('should update active filter', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: {
          index_set_id: [
            { id: 'filter-id', title: 'Default index set', value: 'index-set-1' },
          ],
        },
        onChange: onChangeFiltersWithTitle,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={{ index_set_id: ['index-set-1'] }} />,
      );

      const activeFilter = await screen.findByTestId('filter-filter-id');

      const openSuggestionsButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });

      userEvent.click(openSuggestionsButton);

      userEvent.click(await screen.findByRole('button', {
        name: /example index set/i,
      }));

      await waitFor(() => expect(onChangeFiltersWithTitle).toHaveBeenCalledWith(
        { index_set_id: [{ id: 'filter-id', title: 'Example index set', value: 'index-set-2' }] },
        { index_set_id: ['index-set-2'] },
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith({ index_set_id: ['index-set-2'] }));
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
        {
          created_at: [{
            id: 'filter-id',
            title: '2020-01-01 00:55:00.000 - Now',
            value: '2019-12-31T23:55:00.000+00:00><',
          }],
        },
        { created_at: ['2019-12-31T23:55:00.000+00:00><'] },
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith({ created_at: ['2019-12-31T23:55:00.000+00:00><'] }));
    });

    it('should update active filter', async () => {
      const setUrlQueryFilters = jest.fn();

      asMock(useFiltersWithTitle).mockReturnValue({
        data: {
          created_at: [{
            id: 'filter-id',
            title: '2020-01-01 00:55:00 - Now',
            value: '2019-12-31T23:55:00.001+00:00',
          }],
        },
        onChange: onChangeFiltersWithTitle,
      });

      render(
        <EntityFilters attributes={attributes}
                       setUrlQueryFilters={setUrlQueryFilters}
                       urlQueryFilters={{ created_at: ['2019-12-31T23:55:00.000+00:00><'] }} />,
      );

      const activeFilter = await screen.findByTestId('filter-filter-id');

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
        {
          created_at: [{
            id: 'filter-id',
            title: '2020-01-01 00:55:00.001 - Now',
            value: '2019-12-31T23:55:00.001+00:00><',
          }],
        },
        { created_at: ['2019-12-31T23:55:00.001+00:00><'] },
      ));

      await waitFor(() => expect(setUrlQueryFilters).toHaveBeenCalledWith({ created_at: ['2019-12-31T23:55:00.001+00:00><'] }));
    });
  });

  it('should display active filters', async () => {
    const setUrlQueryFilters = jest.fn();

    asMock(useFiltersWithTitle).mockReturnValue({
      data: {
        disabled: [{ id: 'filter-id', title: 'Running', value: 'false' }],
      },
      onChange: onChangeFiltersWithTitle,
    });

    render(
      <EntityFilters attributes={attributes}
                     setUrlQueryFilters={setUrlQueryFilters}
                     urlQueryFilters={{ disabled: ['false'] }} />,
    );

    await screen.findByTestId('filter-filter-id');
  });
});
