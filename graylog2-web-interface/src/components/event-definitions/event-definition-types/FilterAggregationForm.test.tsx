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
import userEvent from '@testing-library/user-event';

import FormWarningsProvider from 'contexts/FormWarningsProvider';
import { simpleEventDefinition } from 'fixtures/eventDefinition';
import { adminUser } from 'fixtures/users';

import FilterAggregationForm from './FilterAggregationForm';

describe('FilterAggregationForm', () => {
  const defaultProps = {
    eventDefinition: simpleEventDefinition,
    onChange: jest.fn(),
    entityTypes: { aggregation_functions: [] },
    streams: [],
    currentUser: adminUser,
    validation: { errors: {} },
  };

  const aggregationEventDefinition = {
    ...simpleEventDefinition,
    config: {
      ...simpleEventDefinition.config,
      group_by: ['source'],
      series: [{ id: 'series-1', function: 'count', field: 'timestamp' }],
      conditions: {
        expression: {
          expr: '<',
          left: { expr: 'number-ref', ref: 'series-1' },
          right: { expr: 'number', value: 100 },
        },
      },
    },
  };

  const renderForm = (props = {}) => render(
    <FormWarningsProvider>
      <FilterAggregationForm {...defaultProps} {...props} />
    </FormWarningsProvider>,
  );

  it('should clear aggregation config when switching from aggregation to filter mode', async () => {
    const onChange = jest.fn();

    renderForm({ eventDefinition: aggregationEventDefinition, onChange });

    const filterRadio = await screen.findByLabelText('Filter has results');

    await userEvent.click(filterRadio);

    expect(onChange).toHaveBeenCalledWith('config', expect.objectContaining({
      group_by: [],
      series: [],
      conditions: {},
    }));
  });

  it('should restore aggregation config when switching back from filter to aggregation mode', async () => {
    const onChange = jest.fn();

    const { rerender } = renderForm({
      eventDefinition: aggregationEventDefinition,
      onChange,
    });

    // Switch to filter mode
    const filterRadio = await screen.findByLabelText('Filter has results');

    await userEvent.click(filterRadio);

    expect(onChange).toHaveBeenCalledWith('config', expect.objectContaining({
      group_by: [],
      series: [],
      conditions: {},
    }));

    // Simulate parent updating eventDefinition with the cleared config
    const clearedConfig = onChange.mock.calls[0][1];
    const clearedEventDefinition = {
      ...aggregationEventDefinition,
      config: clearedConfig,
    };

    rerender(
      <FormWarningsProvider>
        <FilterAggregationForm
          {...defaultProps}
          eventDefinition={clearedEventDefinition}
          onChange={onChange}
        />
      </FormWarningsProvider>,
    );

    // Switch back to aggregation mode
    const aggregationRadio = await screen.findByLabelText('Aggregation of results reaches a threshold');

    await userEvent.click(aggregationRadio);

    expect(onChange).toHaveBeenLastCalledWith('config', expect.objectContaining({
      group_by: ['source'],
      series: [{ id: 'series-1', function: 'count', field: 'timestamp' }],
      conditions: expect.objectContaining({
        expression: expect.objectContaining({ expr: '<' }),
      }),
    }));
  });
});
