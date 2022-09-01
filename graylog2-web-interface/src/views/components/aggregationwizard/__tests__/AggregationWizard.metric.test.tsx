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
import { act, fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import type { PluginRegistration } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { MockStore } from 'helpers/mocking';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldType from 'views/logic/fieldtypes/FieldType';
import dataTable from 'views/components/datatable/bindings';

import AggregationWizard from '../AggregationWizard';

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
  getInitialState: jest.fn(() => ({
    count: { type: 'count', description: 'Count' },
    min: { type: 'min', description: 'Minimum' },
    max: { type: 'max', description: 'Maximum' },
    percentile: { type: 'percentile', description: 'Percentile' },
  })),
  listen: jest.fn(),
}));

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(['getInitialState', () => ({ activeQuery: 'queryId' })]),
}));

const selectEventConfig = { container: document.body };

const plugin: PluginRegistration = { exports: { visualizationTypes: [dataTable] } };

const addElement = async (key: 'Grouping' | 'Metric' | 'Sort') => {
  await userEvent.click(await screen.findByRole('button', { name: 'Add' }));
  await userEvent.click(await screen.findByRole('menuitem', { name: key }));
};

const submitWidgetConfigForm = async () => {
  const applyButton = await screen.findByRole('button', { name: 'Update Preview' });
  fireEvent.click(applyButton);
};

const selectMetric = async (functionName, fieldName, elementIndex = 0) => {
  const newFunctionSelect = screen.getAllByLabelText('Select a function')[elementIndex];
  const newFieldSelect = screen.getAllByLabelText('Select a field')[elementIndex];

  await act(async () => {
    await selectEvent.openMenu(newFunctionSelect);
    await selectEvent.select(newFunctionSelect, functionName, selectEventConfig);
    await selectEvent.openMenu(newFieldSelect);
    await selectEvent.select(newFieldSelect, fieldName, selectEventConfig);
  });
};

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
        {/* eslint-disable-next-line react/jsx-no-useless-fragment */}
        <>The Visualization</>
      </AggregationWizard>
    </FieldTypesContext.Provider>,
  );

  beforeAll(() => PluginStore.register(plugin));

  afterAll(() => PluginStore.unregister(plugin));

  it('should require metric function when adding a metric element', async () => {
    renderSUT();

    await addElement('Metric');

    await screen.findByText('Function is required.');
  });

  it('should require metric field when metric function is not count', async () => {
    renderSUT();

    await addElement('Metric');

    const functionSelect = await screen.findByLabelText('Select a function');
    await selectEvent.openMenu(functionSelect);
    await selectEvent.select(functionSelect, 'Minimum', selectEventConfig);

    await screen.findByText('Field is required for function min.');
  });

  it('should not require metric field when metric function count', async () => {
    const config = widgetConfig
      .toBuilder()
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
    const config = widgetConfig
      .toBuilder()
      .series([Series.create('count')])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    const nameInput = await screen.findByLabelText(/Name/);
    userEvent.type(nameInput, 'New name');
    await selectMetric('Count', 'http_method');
    await submitWidgetConfigForm();

    const updatedSeriesConfig = SeriesConfig.empty().toBuilder().name('New name').build();
    const updatedConfig = widgetConfig
      .toBuilder()
      .series([Series.create('count', 'http_method').toBuilder().config(updatedSeriesConfig).build()])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should update config with percentile metric function', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .series([Series.create('count')])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    await selectMetric('Percentile', 'http_method');
    const percentileInput = await screen.findByLabelText('Select percentile');

    expect(screen.getByText('Percentile is required.')).toBeInTheDocument();

    await selectEvent.openMenu(percentileInput);
    await selectEvent.select(percentileInput, '50', selectEventConfig);

    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .series([Series.create('percentile', 'http_method', 50.0)])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should configure metric with multiple functions', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .series([Series.create('max', 'took_ms')])
      .build();
    renderSUT({ config, onChange: onChangeMock });

    const addMetricButton = await screen.findByRole('button', { name: 'Add a Metric' });
    fireEvent.click(addMetricButton);

    await waitFor(() => expect(screen.getAllByLabelText('Select a function')).toHaveLength(2));
    const newNameInput = screen.getAllByLabelText(/Name/)[1];

    userEvent.type(newNameInput, 'New function');

    await selectMetric('Minimum', 'http_method', 1);
    await submitWidgetConfigForm();

    const updatedConfig = config.toBuilder()
      .series([
        Series.create('max', 'took_ms'),
        Series.create('min', 'http_method').toBuilder()
          .config(SeriesConfig.empty().toBuilder().name('New function').build())
          .build(),
      ])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should remove all metrics', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .series([Series.create('count')])
      .build();
    renderSUT({ config, onChange: onChangeMock });

    const removeMetricElementButton = screen.getByRole('button', { name: 'Remove Metric' });
    userEvent.click(removeMetricElementButton);

    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .series([])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });
});
