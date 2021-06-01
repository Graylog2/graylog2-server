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
import { render, within, screen, waitFor, fireEvent } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldType from 'views/logic/fieldtypes/FieldType';
import dataTable from 'views/components/datatable/bindings';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

import AggregationWizard from '../AggregationWizard';

const extendedTimeout = applyTimeoutMultiplier(15000);

const fieldType = new FieldType('field_type', ['numeric'], []);
const fieldTypeMapping1 = new FieldTypeMapping('took_ms', fieldType);
const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType);
const fields = Immutable.List([fieldTypeMapping1, fieldTypeMapping2]);
const fieldTypes = { all: fields, queryFields: Immutable.Map({ queryId: fields }) };

const pivot0 = Pivot.create(fieldTypeMapping1.name, 'values', { limit: 15 });
const pivot1 = Pivot.create(fieldTypeMapping2.name, 'values', { limit: 15 });

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .rowPivots([pivot0, pivot1])
  .build();

const plugin: PluginRegistration = { exports: { visualizationTypes: [dataTable] } };

const addSortElement = async () => {
  await userEvent.click(await screen.findByRole('button', { name: 'Add' }));
  await userEvent.click(await screen.findByRole('menuitem', { name: 'Sort' }));
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
        <>The Visualization</>
      </AggregationWizard>
    </FieldTypesContext.Provider>,
  );

  beforeAll(() => PluginStore.register(plugin));

  afterAll(() => PluginStore.unregister(plugin));

  it('should display sort element form with values from config', async () => {
    const config = widgetConfig
      .toBuilder()
      .sort([new SortConfig('pivot', 'http_method', Direction.Ascending)])
      .build();

    renderSUT({ config });

    const httpMethodSortContainer = await screen.findByTestId('sort-element-0');

    expect(within(httpMethodSortContainer).getByText('http_method')).toBeInTheDocument();
    expect(within(httpMethodSortContainer).getByText('Ascending')).toBeInTheDocument();
  });

  it('should update configured sort element', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .sort([new SortConfig('pivot', 'http_method', Direction.Ascending)])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    const httpMethodSortContainer = await screen.findByTestId('sort-element-0');

    const sortFieldSelect = within(httpMethodSortContainer).getByLabelText('Select field for sorting');
    const sortDirectionSelect = within(httpMethodSortContainer).getByLabelText('Select direction for sorting');

    await selectEvent.openMenu(sortFieldSelect);
    await selectEvent.select(sortFieldSelect, 'took_ms');

    await selectEvent.openMenu(sortDirectionSelect);
    await selectEvent.select(sortDirectionSelect, 'Descending');

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    userEvent.click(applyButton);

    const updatedConfig = widgetConfig
      .toBuilder()
      .sort([new SortConfig('pivot', 'took_ms', Direction.Descending)])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should configure new sort element', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .sort([])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    await addSortElement();

    const newSortContainer = await screen.findByTestId('sort-element-0');
    const newSortFieldSelect = within(newSortContainer).getByLabelText('Select field for sorting');
    const newSortDirectionSelect = within(newSortContainer).getByLabelText('Select direction for sorting');

    await selectEvent.openMenu(newSortFieldSelect);
    await selectEvent.select(newSortFieldSelect, 'took_ms');
    await selectEvent.openMenu(newSortDirectionSelect);
    await selectEvent.select(newSortDirectionSelect, 'Descending');

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    userEvent.click(applyButton);

    const updatedConfig = widgetConfig
      .toBuilder()
      .sort([
        new SortConfig('pivot', 'took_ms', Direction.Descending),
      ])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should configure another sort element', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .sort([new SortConfig('pivot', 'http_method', Direction.Ascending)])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    const addSortButton = await screen.findByRole('button', { name: 'Add a Sort' });
    userEvent.click(addSortButton);

    const newSortContainer = await screen.findByTestId('sort-element-1');
    const newSortFieldSelect = within(newSortContainer).getByLabelText('Select field for sorting');
    const newSortDirectionSelect = within(newSortContainer).getByLabelText('Select direction for sorting');

    await selectEvent.openMenu(newSortFieldSelect);
    await selectEvent.select(newSortFieldSelect, 'took_ms');
    await selectEvent.openMenu(newSortDirectionSelect);
    await selectEvent.select(newSortDirectionSelect, 'Descending');

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    userEvent.click(applyButton);

    const updatedConfig = widgetConfig
      .toBuilder()
      .sort([
        new SortConfig('pivot', 'http_method', Direction.Ascending),
        new SortConfig('pivot', 'took_ms', Direction.Descending),
      ])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should require field when creating a sort element', async () => {
    renderSUT();

    await addSortElement();

    const newSortContainer = await screen.findByTestId('sort-element-0');
    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    await waitFor(() => expect(within(newSortContainer).getByText('Field is required.')).toBeInTheDocument());
    await waitFor(() => expect(expect(applyButton).toBeDisabled()));
  });

  it('should require direction when creating a sort element', async () => {
    renderSUT();

    await addSortElement();

    const newSortContainer = await screen.findByTestId('sort-element-0');
    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    await waitFor(() => expect(within(newSortContainer).getByText('Direction is required.')).toBeInTheDocument());
    await waitFor(() => expect(expect(applyButton).toBeDisabled()));
  });

  it('should remove sort', async () => {
    const onChangeMock = jest.fn();
    const config = widgetConfig
      .toBuilder()
      .sort([new SortConfig('pivot', 'http_method', Direction.Ascending)])
      .build();

    renderSUT({ config, onChange: onChangeMock });

    const removeSortElementButton = screen.getByRole('button', { name: 'Remove Sort' });
    userEvent.click(removeSortElementButton);

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    userEvent.click(applyButton);

    const updatedConfig = widgetConfig
      .toBuilder()
      .sort([])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  });

  it('should correctly update sort of sort elements', async () => {
    const sort1 = new SortConfig('pivot', 'http_method', Direction.Ascending);
    const sort2 = new SortConfig('pivot', 'took_ms', Direction.Descending);

    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot0, pivot1])
      .sort([sort1, sort2])
      .build();

    const onChange = jest.fn();
    renderSUT({ onChange, config });

    const sortSection = await screen.findByTestId('Sort-section');

    const firstItem = within(sortSection).getByTestId('sort-0-drag-handle');
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have lifted an item/i);
    fireEvent.keyDown(firstItem, { key: 'ArrowDown', keyCode: 40 });
    await screen.findByText(/You have moved the item/i);
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have dropped the item/i);

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const updatedConfig = config
      .toBuilder()
      .sort([sort2, sort1])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);
});
