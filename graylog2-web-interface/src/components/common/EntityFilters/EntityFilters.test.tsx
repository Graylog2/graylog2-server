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

import EntityFilters from './EntityFilters';

jest.mock('logic/generateId', () => jest.fn(() => 'filter-id'));
jest.mock('components/common/EntityFilters/hooks/useFilterValueSuggestions');

describe('<EntityFilters />', () => {
  const attributes = [
    { id: 'title', title: 'Title', sortable: true },
    { id: 'description', title: 'Description', sortable: true },
    {
      id: 'disabled',
      title: 'Status',
      type: 'BOOLEAN',
      sortable: true,
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
      sortable: true,
      title: 'Index set',
      type: 'STRING',
    },
  ] as Attributes;

  describe('boolean attribute', () => {
    it('should create filter', async () => {
      const onChangeFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes}
                       onChangeFilters={onChangeFilters}
                       activeFilters={undefined} />,
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

      await waitFor(() => expect(onChangeFilters).toHaveBeenCalledWith({
        disabled: [{ id: 'filter-id', title: 'Running', value: 'false' }],
      }));
    });

    it('should update active filter on click', async () => {
      const onChangeFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes}
                       onChangeFilters={onChangeFilters}
                       activeFilters={{ disabled: [{ id: 'filter-id', title: 'Running', value: 'false' }] }} />,
      );

      const activeFilter = await screen.findByTestId('filter-filter-id');

      const toggleFilterButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });

      userEvent.click(toggleFilterButton);

      await waitFor(() => expect(onChangeFilters).toHaveBeenCalledWith({
        disabled: [{ id: 'filter-id', title: 'Paused', value: 'true' }],
      }));
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
      const onChangeFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes}
                       onChangeFilters={onChangeFilters}
                       activeFilters={undefined} />,
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

      await waitFor(() => expect(onChangeFilters).toHaveBeenCalledWith({
        index_set_id: [{ id: 'filter-id', title: 'Default index set', value: 'index-set-1' }],
      }));
    });

    it('should update active filter', async () => {
      const onChangeFilters = jest.fn();

      render(
        <EntityFilters attributes={attributes}
                       onChangeFilters={onChangeFilters}
                       activeFilters={{
                         index_set_id: [
                           { id: 'filter-id', title: 'Default index set', value: 'index-set-1' },
                         ],
                       }} />,
      );

      const activeFilter = await screen.findByTestId('filter-filter-id');

      const openSuggestionsButton = within(activeFilter).getByRole('button', {
        name: /change filter value/i,
      });

      userEvent.click(openSuggestionsButton);

      userEvent.click(await screen.findByRole('button', {
        name: /example index set/i,
      }));

      await waitFor(() => expect(onChangeFilters).toHaveBeenCalledWith({
        index_set_id: [{ id: 'filter-id', title: 'Example index set', value: 'index-set-2' }],
      }));
    });
  });

  it('should display active filters', async () => {
    const onChangeFilters = jest.fn();

    render(
      <EntityFilters attributes={attributes}
                     onChangeFilters={onChangeFilters}
                     activeFilters={{ disabled: [{ id: 'filter-id', title: 'Running', value: 'false' }] }} />,
    );

    await screen.findByTestId('filter-filter-id');
  });
});
