import React from 'react';
import { mount } from 'enzyme';

import NumberRefExpression from './NumberRefExpression';

describe('NumberRefExpression', () => {
  const eventDefinition = (series = []) => {
    return {
      config: {
        series: series,
      },
    };
  };

  const aggregationFunctions = ['avg', 'card'];
  const formattedFields = [
    { label: 'source - string', value: 'source' },
    { label: 'took_ms - long', value: 'took_ms' },
  ];

  it('should have no selected function and field with an undefined ref', () => {
    const expression = {
      expr: 'number-ref',
      ref: undefined,
    };

    const wrapper = mount(
      <NumberRefExpression eventDefinition={eventDefinition()}
                           aggregationFunctions={aggregationFunctions}
                           expression={expression}
                           formattedFields={formattedFields}
                           onChange={() => { }}
                           renderLabel={false}
                           validation={{ errors: {} }} />,
    );

    const functionSelect = wrapper.find('Select.aggregation-function').at(0);
    expect(functionSelect.prop('value')).toBe(undefined);

    const fieldSelect = wrapper.find('Select.aggregation-function-field').at(0);
    expect(fieldSelect.prop('value')).toBe(undefined);
  });

  it('should get the right series when ref is set', () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const series = [
      { id: 'avg-took_ms', function: 'avg', field: 'took_ms' },
    ];

    const wrapper = mount(
      <NumberRefExpression eventDefinition={eventDefinition(series)}
                           aggregationFunctions={aggregationFunctions}
                           expression={expression}
                           formattedFields={formattedFields}
                           onChange={() => { }}
                           renderLabel={false}
                           validation={{ errors: {} }} />,
    );

    const functionSelect = wrapper.find('Select.aggregation-function').at(0);
    expect(functionSelect.prop('value')).toBe('avg');

    const fieldSelect = wrapper.find('Select.aggregation-function-field').at(0);
    expect(fieldSelect.prop('value')).toBe('took_ms');
  });

  it('should update ref and add series when function changes', () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const initialSeries = { id: 'avg-took_ms', function: 'avg', field: 'took_ms' };
    const definition = eventDefinition([initialSeries]);
    const handleChange = jest.fn(({ conditions, series }) => {
      expect(conditions).toBeDefined();
      expect(conditions.ref).toBe('card-took_ms');
      expect(series).toBeDefined();
      expect(series).toHaveLength(2);
      expect(series).toContainEqual({ id: 'card-took_ms', function: 'card', field: 'took_ms' });
      expect(series).toContainEqual(initialSeries);
    });

    const wrapper = mount(
      <NumberRefExpression eventDefinition={definition}
                           aggregationFunctions={aggregationFunctions}
                           expression={expression}
                           formattedFields={formattedFields}
                           onChange={handleChange}
                           renderLabel={false}
                           validation={{ errors: {} }} />,
    );

    const functionSelect = wrapper.find('Select Select.aggregation-function').at(0);
    functionSelect.prop('onChange')({ value: 'card' });
    expect(handleChange.mock.calls.length).toBe(1);
  });

  it('should update ref and series when field changes', () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const initialSeries = { id: 'avg-took_ms', function: 'avg', field: 'took_ms' };
    const definition = eventDefinition([initialSeries]);
    const handleChange = jest.fn(({ conditions, series }) => {
      expect(conditions).toBeDefined();
      expect(conditions.ref).toBe('avg-source');
      expect(series).toBeDefined();
      expect(series).toHaveLength(2);
      expect(series).toContainEqual({ id: 'avg-source', function: 'avg', field: 'source' });
      expect(series).toContainEqual(initialSeries);
    });

    const wrapper = mount(
      <NumberRefExpression eventDefinition={definition}
                           aggregationFunctions={aggregationFunctions}
                           expression={expression}
                           formattedFields={formattedFields}
                           onChange={handleChange}
                           renderLabel={false}
                           validation={{ errors: {} }} />,
    );

    const fieldSelect = wrapper.find('Select Select.aggregation-function-field').at(0);
    fieldSelect.prop('onChange')({ value: 'source' }, { action: 'select-option' });
    expect(handleChange.mock.calls.length).toBe(1);
  });

  it('should update ref and series when field is unset', () => {
    const expression = {
      expr: 'number-ref',
      ref: 'avg-took_ms',
    };
    const initialSeries = { id: 'avg-took_ms', function: 'avg', field: 'took_ms' };
    const definition = eventDefinition([initialSeries]);
    const handleChange = jest.fn(({ conditions, series }) => {
      expect(conditions).toBeDefined();
      expect(conditions.ref).toBe('avg-source');
      expect(series).toBeDefined();
      expect(series).toHaveLength(2);
      expect(series).toContainEqual({ id: 'avg-source', function: 'avg', field: 'source' });
      expect(series).toContainEqual(initialSeries);
    });

    const wrapper = mount(
      <NumberRefExpression eventDefinition={definition}
                           aggregationFunctions={aggregationFunctions}
                           expression={expression}
                           formattedFields={formattedFields}
                           onChange={handleChange}
                           renderLabel={false}
                           validation={{ errors: {} }} />,
    );

    const fieldSelect = wrapper.find('Select Select.aggregation-function-field').at(0);
    fieldSelect.prop('onChange')({ value: 'source' }, { action: 'select-option' });
    expect(handleChange.mock.calls.length).toBe(1);
  });
});
