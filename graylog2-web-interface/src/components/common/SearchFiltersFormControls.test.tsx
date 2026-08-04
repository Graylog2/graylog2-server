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
import { render, screen } from 'wrappedTestingLibrary';
import { useFormikContext } from 'formik';
import type { OrderedMap } from 'immutable';

import asMock from 'helpers/mocking/AsMock';
import usePluginEntities from 'hooks/usePluginEntities';
import type { SearchFilter } from 'components/event-definitions/event-definitions-types';

import SearchFiltersFormControls from './SearchFiltersFormControls';

jest.mock('hooks/usePluginEntities');

// Renders the form state the pluggable search filter bar is working with. All filter actions (edit, negate,
// remove) address a filter by its `frontendId`, so it has to match the key it is stored under.
const FormStateProbe = () => {
  const {
    values: { searchFilters },
  } = useFormikContext<{ searchFilters: OrderedMap<string, SearchFilter> }>();

  return (
    <ul>
      {searchFilters
        .entrySeq()
        .map(([key, filter]) => (
          <li key={key}>{`${filter.title}: ${key === filter.frontendId ? 'matching' : 'mismatching'}`}</li>
        ))
        .toArray()}
    </ul>
  );
};

describe('SearchFiltersFormControls', () => {
  const filter = (overrides: Partial<SearchFilter>): SearchFilter => ({
    id: undefined,
    type: 'inlineQueryString',
    title: 'Filter',
    queryString: 'http_method:POST',
    disabled: false,
    negation: false,
    ...overrides,
  });

  beforeEach(() => {
    asMock(usePluginEntities).mockImplementation(
      (entityKey) =>
        ({
          'eventDefinitions.components.searchForm': [() => ({ id: 'search-filters', component: FormStateProbe })],
        })[entityKey],
    );
  });

  it('keys referenced filters by their id', async () => {
    render(
      <SearchFiltersFormControls
        filters={[filter({ id: 'filter-id', type: 'referenced', title: 'Referenced filter' })]}
        onChange={() => {}}
      />,
    );

    expect(await screen.findByText('Referenced filter: matching')).toBeInTheDocument();
  });

  it('assigns the same generated id as key and frontendId for filters without an id', async () => {
    render(
      <SearchFiltersFormControls
        filters={[filter({ title: 'First inline filter' }), filter({ title: 'Second inline filter' })]}
        onChange={() => {}}
      />,
    );

    expect(await screen.findByText('First inline filter: matching')).toBeInTheDocument();
    expect(await screen.findByText('Second inline filter: matching')).toBeInTheDocument();
  });
});
