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

import NumberRefExpression from './NumberRefExpression';

describe('NumberRefExpression', () => {
  const eventDefinition = (series = []) => ({
    config: {
      series: series,
    },
  });

  const aggregationFunctions = ['avg', 'card'];
  const formattedFields = [
    { label: 'source - string', value: 'source' },
    { label: 'took_ms - long', value: 'took_ms' },
  ];

  it('should have no selected function and field with an undefined ref', async () => {
    const expression = {
      expr: 'number-ref',
      ref: undefined,
    };

    render(
      <NumberRefExpression
        eventDefinition={eventDefinition()}
        aggregationFunctions={aggregationFunctions}
        expression={expression}
        formattedFields={formattedFields}
        onChange={() => {}}
        renderLabel={false}
        validation={{ errors: {} }}
      />,
    );

    expect(await screen.findByRole('combobox', { name: /select function/i })).toHaveValue('');
    expect(await screen.findByRole('combobox', { name: /select field/i })).toHaveValue('');
  });

  it('should get the right series when ref is set', async () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const series = [{ id: 'avg-took_ms', type: 'avg', field: 'took_ms' }];

    render(
      <NumberRefExpression
        eventDefinition={eventDefinition(series)}
        aggregationFunctions={aggregationFunctions}
        expression={expression}
        formattedFields={formattedFields}
        onChange={() => {}}
        renderLabel={false}
        validation={{ errors: {} }}
      />,
    );

    await screen.findByText(/avg\(\)/i);
    await screen.findByText(/took_ms - long/i);
  });

  it('should update ref and add series when function changes', async () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const initialSeries = { id: 'avg-took_ms', type: 'avg', field: 'took_ms' };
    const definition = eventDefinition([initialSeries]);
    const handleChange = jest.fn();

    render(
      <NumberRefExpression
        eventDefinition={definition}
        aggregationFunctions={aggregationFunctions}
        expression={expression}
        formattedFields={formattedFields}
        onChange={handleChange}
        renderLabel={false}
        validation={{ errors: {} }}
      />,
    );

    const functionSelect = await screen.findByRole('combobox', { name: /select function/i });
    await userEvent.type(functionSelect, 'card{enter}');

    expect(handleChange).toHaveBeenCalledWith({
      conditions: { expr: 'number-ref', ref: 'card-took_ms' },
      series: [
        { field: 'took_ms', id: 'avg-took_ms', type: 'avg' },
        { field: 'took_ms', id: 'card-took_ms', type: 'card' },
      ],
    });
  });

  it('should update ref and series when field changes', async () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const initialSeries = { id: 'avg-took_ms', type: 'avg', field: 'took_ms' };
    const definition = eventDefinition([initialSeries]);
    const handleChange = jest.fn();

    render(
      <NumberRefExpression
        eventDefinition={definition}
        aggregationFunctions={aggregationFunctions}
        expression={expression}
        formattedFields={formattedFields}
        onChange={handleChange}
        renderLabel={false}
        validation={{ errors: {} }}
      />,
    );

    const functionSelect = await screen.findByRole('combobox', { name: /select field/i });
    await userEvent.type(functionSelect, 'source{enter}');

    expect(handleChange).toHaveBeenCalledWith({
      conditions: { expr: 'number-ref', ref: 'avg-source' },
      series: [
        { field: 'took_ms', id: 'avg-took_ms', type: 'avg' },
        { field: 'source', id: 'avg-source', type: 'avg' },
      ],
    });
  });

  it('should update ref and series when field is unset', async () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const initialSeries = { id: 'avg-took_ms', type: 'avg', field: 'took_ms' };
    const definition = eventDefinition([initialSeries]);
    const handleChange = jest.fn();

    render(
      <NumberRefExpression
        eventDefinition={definition}
        aggregationFunctions={aggregationFunctions}
        expression={expression}
        formattedFields={formattedFields}
        onChange={handleChange}
        renderLabel={false}
        validation={{ errors: {} }}
      />,
    );

    const functionSelect = await screen.findByRole('combobox', { name: /select field/i });
    await userEvent.type(functionSelect, 'source{enter}');

    expect(handleChange).toHaveBeenCalledWith({
      conditions: { expr: 'number-ref', ref: 'avg-source' },
      series: [
        { field: 'took_ms', id: 'avg-took_ms', type: 'avg' },
        { field: 'source', id: 'avg-source', type: 'avg' },
      ],
    });
  });
});
