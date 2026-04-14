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
import userEvent from '@testing-library/user-event';
import { render, screen } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import { simpleEventDefinition } from 'fixtures/eventDefinition';

import FilterAggregationForm from './FilterAggregationForm';

jest.mock('./FilterForm', () => () => <div>Filter form</div>);
jest.mock('./FilterPreviewContainer', () => () => <div>Filter preview</div>);
jest.mock('./AggregationForm', () => () => <div>Aggregation form</div>);

describe('FilterAggregationForm', () => {
  const aggregationConfig = {
    ...simpleEventDefinition.config,
    group_by: ['source'],
    series: [{ id: 'count()', type: 'count', field: '' }],
    conditions: { expression: { expr: 'number', value: 5 } },
    event_limit: 1000,
  };

  const renderForm = (config = aggregationConfig, onChange = jest.fn()) =>
    render(
      <FilterAggregationForm
        entityTypes={{ aggregation_functions: [] }}
        eventDefinition={{ ...simpleEventDefinition, config }}
        streams={[]}
        validation={{ errors: {} }}
        currentUser={defaultUser}
        onChange={onChange}
      />,
    );

  it('clears aggregation settings when switching to filter', async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderForm(aggregationConfig, onChange);

    await user.click(screen.getByLabelText('Filter has results'));

    expect(onChange).toHaveBeenCalledWith(
      'config',
      expect.objectContaining({
        group_by: [],
        series: [],
        conditions: { expression: null },
      }),
    );
  });

  it('restores previous aggregation settings when switching back to aggregation', async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();

    renderForm(aggregationConfig, onChange);

    await user.click(screen.getByLabelText('Filter has results'));
    await user.click(screen.getByLabelText('Aggregation of results reaches a threshold'));

    expect(onChange).toHaveBeenLastCalledWith(
      'config',
      expect.objectContaining({
        group_by: aggregationConfig.group_by,
        series: aggregationConfig.series,
        conditions: aggregationConfig.conditions,
      }),
    );
  });
});
