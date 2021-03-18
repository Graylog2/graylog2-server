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
import * as Immutable from 'immutable';
import { act, fireEvent, render, screen, waitFor, within } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldType from 'views/logic/fieldtypes/FieldType';

import AggregationWizard from './AggregationWizard';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .build();

const fieldType = new FieldType('field_type', ['numeric'], []);
const fieldTypeMapping1 = new FieldTypeMapping('took_ms', fieldType);
const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType);
const fields = Immutable.List([fieldTypeMapping1, fieldTypeMapping2]);
const fieldTypes = { all: fields, queryFields: Immutable.Map({ queryId: fields }) };

jest.mock('views/stores/AggregationFunctionsStore', () => ({
  getInitialState: jest.fn(() => ({ count: { type: 'count' }, min: { type: 'min' }, max: { type: 'max' }, percentile: { type: 'percentile' } })),
  listen: jest.fn(),
}));

describe('AggregationWizard', () => {
  const renderSUT = (props = {}) => render(
    <FieldTypesContext.Provider value={fieldTypes}>
      <AggregationWizard onChange={() => {}}
                         config={widgetConfig}
                         editing
                         id="widget-id"
                         type="AGGREGATION"
                         fields={Immutable.List([])}
                         {...props}>
        The Visualization
      </AggregationWizard>
    </FieldTypesContext.Provider>,
  );

  it('should render visualization passed as children', () => {
    renderSUT();

    expect(screen.getByText('The Visualization')).toBeInTheDocument();
  });

  it('should list available aggregation elements in element select', async () => {
    const config = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .build();

    renderSUT({ config });

    const addElementSection = await screen.findByTestId('add-element-section');
    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');
    const notConfiguredElements = [
      'Metric',
      'Group By',
      'Sort',
    ];

    await selectEvent.openMenu(aggregationElementSelect);

    notConfiguredElements.forEach((elementTitle) => {
      expect(within(addElementSection).getByText(elementTitle)).toBeInTheDocument();
    });
  });

  it('should display newly selected aggregation element', async () => {
    renderSUT();

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');
    const configureElementsSection = screen.getByTestId('configure-elements-section');

    expect(within(configureElementsSection).queryByText('Metric')).not.toBeInTheDocument();

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Metric');

    await waitFor(() => expect(within(configureElementsSection).getByText('Metric')).toBeInTheDocument());
  });

  it('should require metric function when adding a metric', async () => {
    renderSUT();

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Metric');

    await waitFor(() => expect(screen.getByText('Function is required.')).toBeInTheDocument());
  });

  it('should require metric field when metric function is not count', async () => {
    renderSUT();

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Metric');

    const functionSelect = await screen.findByLabelText('Select a function');
    await selectEvent.openMenu(functionSelect);
    await selectEvent.select(functionSelect, 'min');

    await waitFor(() => expect(screen.getByText('Field is required for function min.')).toBeInTheDocument());
  });

  it('should not require metric field when metric function count', async () => {
    const config = AggregationWidgetConfig
      .builder()
      .series([Series.create('count')])
      .build();
    renderSUT({ config });

    await waitFor(() => expect(screen.queryByText('Field is required for function min.')).not.toBeInTheDocument());
  });

  it('should display metric form with values from config', async () => {
    const updatedSeriesConfig = SeriesConfig.empty().toBuilder().name('Metric name').build();
    const config = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .series([Series.create('max', 'took_ms').toBuilder().config(updatedSeriesConfig).build()])
      .build();

    renderSUT({ config });

    await screen.findByDisplayValue('Metric name');

    expect(screen.getByDisplayValue('took_ms')).toBeInTheDocument();
    expect(screen.getByDisplayValue('max')).toBeInTheDocument();
  });

  it('should update config with updated metric', async () => {
    const onChangeMock = jest.fn();
    const config = AggregationWidgetConfig
      .builder()
      .series([Series.create('count')])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    const nameInput = await screen.findByLabelText('Name');
    const functionSelect = screen.getByLabelText('Select a function');
    const fieldSelect = screen.getByLabelText('Select a field');

    userEvent.type(nameInput, 'New name');

    await act(async () => {
      await selectEvent.openMenu(functionSelect);
      await selectEvent.select(functionSelect, 'count');
      await selectEvent.openMenu(fieldSelect);
      await selectEvent.select(fieldSelect, 'http_method');
    });

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const updatedSeriesConfig = SeriesConfig.empty().toBuilder().name('New name').build();
    const updatedConfig = AggregationWidgetConfig
      .builder()
      .series([Series.create('count', 'http_method').toBuilder().config(updatedSeriesConfig).build()])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should update config with percentile metric function', async () => {
    const onChangeMock = jest.fn();
    const config = AggregationWidgetConfig
      .builder()
      .series([Series.create('count')])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    const functionSelect = screen.getByLabelText('Select a function');
    const fieldSelect = screen.getByLabelText('Select a field');

    await act(async () => {
      await selectEvent.openMenu(functionSelect);
      await selectEvent.select(functionSelect, 'percentile');
      await selectEvent.openMenu(fieldSelect);
      await selectEvent.select(fieldSelect, 'http_method');
    });

    const percentileInput = await screen.findByLabelText('Select percentile');

    expect(screen.getByText('Percentile is required.')).toBeInTheDocument();

    await selectEvent.openMenu(percentileInput);
    await selectEvent.select(percentileInput, '50');

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const updatedConfig = AggregationWidgetConfig
      .builder()
      .series([Series.create('percentile', 'http_method', 50.0).toBuilder().build()])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });
});
