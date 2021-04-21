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
import { act, findByTestId, fireEvent, render, screen, waitFor, within } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import FieldType, { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import dataTable from 'views/components/datatable/bindings';

import AggregationWizard from '../AggregationWizard';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .build();

const fieldType = new FieldType('field_type', ['numeric'], []);
const fieldTypeMapping1 = new FieldTypeMapping('took_ms', fieldType);
const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType);
const fieldTypeMapping3 = new FieldTypeMapping('timestamp', FieldTypes.DATE());
const fields = Immutable.List([fieldTypeMapping1, fieldTypeMapping2, fieldTypeMapping3]);
const fieldTypes = { all: fields, queryFields: Immutable.Map({ queryId: fields }) };

const plugin: PluginRegistration = { exports: { visualizationTypes: [dataTable] } };

const addElement = async (key: 'Grouping' | 'Metric' | 'Sort') => {
  await userEvent.click(await screen.findByRole('button', { name: 'Add' }));
  await userEvent.click(await screen.findByRole('menuitem', { name: key }));
};

describe('AggregationWizard', () => {
  const renderSUT = (props = {}) => render(
    <FieldTypesContext.Provider value={fieldTypes}>
      <AggregationWizard config={widgetConfig}
                         editing
                         id="widget-id"
                         type="AGGREGATION"
                         fields={Immutable.List([])}
                         onChange={() => {}}
                         {...props}>
        The Visualization
      </AggregationWizard>,
    </FieldTypesContext.Provider>,
  );

  beforeAll(() => PluginStore.register(plugin));

  afterAll(() => PluginStore.unregister(plugin));

  it('should require group by function when adding a group by element', async () => {
    renderSUT();

    await userEvent.click(await screen.findByRole('button', { name: 'Add' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Grouping' }));

    await waitFor(() => expect(screen.getByText('Field is required.')).toBeInTheDocument());
  });

  it('should change the config when applied', async () => {
    const onChange = jest.fn();
    renderSUT({ onChange });

    await addElement('Grouping');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'took_ms');
    });

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const pivot = Pivot.create('took_ms', 'values', { limit: 15 });
    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  });

  it('should handle timestamp field types', async () => {
    renderSUT();

    await addElement('Grouping');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'timestamp');
    });

    const autoCheckbox = await screen.findByRole('checkbox', { name: 'Auto' });
    await screen.findByRole('slider', { name: /interval/i });

    await userEvent.click(autoCheckbox);

    await screen.findByRole('button', { name: /minutes/i });
  });

  it('should create group by with multiple groupings', async () => {
    const onChange = jest.fn();
    renderSUT({ onChange });

    await addElement('Grouping');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'timestamp');
    });

    await addElement('Grouping');

    const fieldSelections = await screen.findAllByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelections[1]);
      await selectEvent.select(fieldSelections[1], 'took_ms');
    });

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const pivot0 = Pivot.create('timestamp', 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.create('took_ms', 'values', { limit: 15 });
    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot0, pivot1])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  });

  it('should display group by with values from config', async () => {
    const pivot0 = Pivot.create('timestamp', 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.create('took_ms', 'values', { limit: 15 });
    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot0, pivot1])
      .build();

    renderSUT({ config });

    await screen.findByText('took_ms');
    await screen.findByText('timestamp');
  });

  it('should display group by section even if config has no pivots', () => {
    const config = widgetConfig
      .toBuilder()
      .build();

    renderSUT({ config });

    const configureElementsSection = screen.getByTestId('configure-elements-section');

    expect(within(configureElementsSection).queryByText('Group By')).toBeInTheDocument();
  });

  it('should correctly change config', async () => {
    const pivot0 = Pivot.create('timestamp', 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.create('took_ms', 'values', { limit: 15 });
    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot0])
      .build();

    const onChange = jest.fn();
    renderSUT({ onChange, config });

    await screen.findByText('timestamp');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'took_ms');
    });

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot1])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  });

  it('should correctly update sort of groupings', async () => {
    const pivot0 = Pivot.create('timestamp', 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.create('took_ms', 'values', { limit: 15 });
    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot0, pivot1])
      .build();

    const onChange = jest.fn();
    renderSUT({ onChange, config });

    const groupBySection = await screen.findByTestId('Group By-section');

    const firstItem = within(groupBySection).getByTestId('grouping-0-drag-handle');
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have lifted an item/i);
    fireEvent.keyDown(firstItem, { key: 'ArrowDown', keyCode: 40 });
    await screen.findByText(/You have moved the item/i);
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have dropped the item/i);

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot1, pivot0])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  });
});
