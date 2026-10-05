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
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import type { Attribute } from 'stores/PaginationTypes';

import EventDefinitionTypeFilter from './EventDefinitionTypeFilter';

jest.mock('hooks/usePluginEntities');

const TYPES = [
  { type: 'sigma-v1', displayName: 'Sigma Rule', useCondition: () => true },
  { type: 'aggregation-v1', displayName: 'Filter & Aggregation', useCondition: () => true },
  // Unlicensed enterprise type — should be filtered out by useCondition().
  { type: 'traffic-v1', displayName: 'License Usage Monitoring', useCondition: () => false },
];

const attribute = { id: 'type', title: 'Type', type: 'STRING', filterable: true } as Attribute;

describe('EventDefinitionTypeFilter', () => {
  const onSubmit = jest.fn();

  const renderFilter = (props: Partial<React.ComponentProps<typeof EventDefinitionTypeFilter>> = {}) =>
    render(
      <EventDefinitionTypeFilter
        attribute={attribute}
        allActiveFilters={undefined}
        filterValueRenderer={(_value, title) => title}
        onSubmit={onSubmit}
        {...props}
      />,
    );

  beforeEach(() => {
    onSubmit.mockClear();
    asMock(usePluginEntities).mockReturnValue(TYPES);
  });

  it('lists registered type display names sorted alphabetically', () => {
    const { container } = renderFilter();

    expect(screen.getByText('Filter & Aggregation')).toBeInTheDocument();
    expect(screen.getByText('Sigma Rule')).toBeInTheDocument();

    // Unlicensed type (useCondition: () => false) is excluded.
    expect(screen.queryByText('License Usage Monitoring')).not.toBeInTheDocument();

    // 'Filter & Aggregation' sorts before 'Sigma Rule' despite the reversed input order.
    const html = container.innerHTML;
    expect(html.indexOf('Filter &amp; Aggregation')).toBeLessThan(html.indexOf('Sigma Rule'));
  });

  it('submits the type id (config.type value) when a suggestion is clicked', async () => {
    renderFilter();

    await userEvent.click(screen.getByText('Sigma Rule'));

    expect(onSubmit).toHaveBeenCalledWith({ value: 'sigma-v1', title: 'Sigma Rule' }, true);
  });
});
