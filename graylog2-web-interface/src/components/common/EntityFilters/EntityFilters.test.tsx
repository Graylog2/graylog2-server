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
import type { Optional } from 'utility-types';

import type { Attributes } from 'stores/PaginationTypes';
import { asMock } from 'helpers/mocking';
import useFilterValueSuggestions from 'components/common/EntityFilters/hooks/useFilterValueSuggestions';
import useFiltersWithTitle from 'components/common/EntityFilters/hooks/useFiltersWithTitle';

import OriginalEntityFilters from './EntityFilters';

const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.useFakeTimers()
  .setSystemTime(mockedUnixTime);

jest.mock('logic/generateId', () => jest.fn(() => 'filter-id'));
jest.mock('components/common/EntityFilters/hooks/useFilterValueSuggestions');
jest.mock('components/common/EntityFilters/hooks/useFiltersWithTitle');

describe('<EntityFilters />', () => {
  const onChangeFiltersWithTitle = jest.fn();
  const setUrlQueryFilters = jest.fn();
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
    {
      id: 'generic',
      filterable: true,
      title: 'Generic Attribute',
      type: 'STRING',
    },
  ] as Attributes;

  const EntityFilters = (props: Optional<React.ComponentProps<typeof OriginalEntityFilters>, 'setUrlQueryFilters' | 'attributes'>) => (
    <OriginalEntityFilters setUrlQueryFilters={setUrlQueryFilters} attributes={attributes} {...props} />
  );

  const dropdownIsHidden = (dropdownTitle: string) => expect(screen.queryByRole('heading', { name: new RegExp(dropdownTitle, 'i') })).not.toBeInTheDocument();

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
      render(
        <EntityFilters urlQueryFilters={undefined} />,
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
      await waitFor(() => dropdownIsHidden('create filter'));
    });

    it('should update active filter on click', async () => {
      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({ disabled: [{ title: 'Running', value: 'false' }] }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
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
        <EntityFilters urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
      );

      await screen.findByTestId('disabled-filter-false');

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      const statusElement = await screen.findByRole('menuitem', { name: /status/i });

      await waitFor(() => expect(statusElement).toBeDisabled());
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
      render(
        <EntityFilters urlQueryFilters={undefined} />,
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
      await waitFor(() => dropdownIsHidden('create filter'));
    });

    it('should update active filter', async () => {
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
        <EntityFilters urlQueryFilters={OrderedMap({ index_set_id: ['index-set-1'] })} />,
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
      await waitFor(() => dropdownIsHidden('edit index set filter'));
    });
  });

  describe('date attribute', () => {
    it('should create filter', async () => {
      render(
        <EntityFilters urlQueryFilters={undefined} />,
      );

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /created at/i,
      }));

      const timeRangeForm = await screen.findByTestId('time-range-form');

      const fromInput = within(timeRangeForm).getByRole('textbox', { name: /from/i });
      userEvent.clear(fromInput);
      userEvent.paste(fromInput, '2020-01-01 00:55:00.000');

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
      await waitFor(() => dropdownIsHidden('create created filter'));
    });

    it('should update active filter', async () => {
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
        <EntityFilters urlQueryFilters={OrderedMap({ created_at: ['2019-12-31T23:55:00.000+00:00><'] })} />,
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
      await waitFor(() => dropdownIsHidden('edit created filter'));
    });
  });

  describe('string attribute', () => {
    it('should prevent creating same filter multiple times', async () => {
      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({ type: [{ title: 'String', value: 'string' }] }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters urlQueryFilters={OrderedMap({ type: ['string'] })} />,
      );

      await screen.findByTestId('type-filter-string');

      userEvent.click(await screen.findByRole('button', {
        name: /create filter/i,
      }));

      userEvent.click(await screen.findByRole('menuitem', {
        name: /type/i,
      }));

      expect(screen.getByRole('menuitem', { name: /string/i })).toBeDisabled();
    });
  });

  describe('generic attribute', () => {
    it('provides text input to create filter', async () => {
      render(
        <EntityFilters urlQueryFilters={OrderedMap()} />,
      );

      userEvent.click(await screen.findByRole('button', { name: /create filter/i }));

      userEvent.click(await screen.findByRole('menuitem', { name: /generic/i }));

      const filterInput = await screen.findByPlaceholderText('Enter value to filter for');
      userEvent.type(filterInput, 'foo');

      const form = await screen.findByTestId('generic-filter-form');
      userEvent.click(await within(form).findByRole('button', { name: /create filter/i }));

      await waitFor(() => {
        expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ generic: ['foo'] }));
      });
    });

    it('allows changing filter', async () => {
      asMock(useFiltersWithTitle).mockReturnValue({
        data: OrderedMap({ generic: [{ title: 'foo', value: 'foo' }] }),
        onChange: onChangeFiltersWithTitle,
        isInitialLoading: false,
      });

      render(
        <EntityFilters urlQueryFilters={OrderedMap()} />,
      );

      userEvent.click(await screen.findByText('foo'));

      const filterInput = await screen.findByPlaceholderText('Enter value to filter for');
      userEvent.type(filterInput, '{selectall}bar');

      const form = await screen.findByTestId('generic-filter-form');
      userEvent.click(await within(form).findByRole('button', { name: /update filter/i }));

      await waitFor(() => {
        expect(setUrlQueryFilters).toHaveBeenCalledWith(OrderedMap({ generic: ['bar'] }));
      });
    });
  });

  it('should display active filters', async () => {
    asMock(useFiltersWithTitle).mockReturnValue({
      data: OrderedMap({
        disabled: [{ title: 'Running', value: 'false' }],
      }),
      onChange: onChangeFiltersWithTitle,
      isInitialLoading: false,
    });

    render(
      <EntityFilters urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
    );

    await screen.findByTestId('disabled-filter-false');
  });

  it('should delete an active filter', async () => {
    asMock(useFiltersWithTitle).mockReturnValue({
      data: OrderedMap({
        disabled: [{ title: 'Running', value: 'false' }],
      }),
      onChange: onChangeFiltersWithTitle,
      isInitialLoading: false,
    });

    render(
      <EntityFilters urlQueryFilters={OrderedMap({ disabled: ['false'] })} />,
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
