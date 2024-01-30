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
import type { PluginRegistration } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import type { Map } from 'immutable';

import { asMock } from 'helpers/mocking';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable, { bindings as dataTable } from 'views/components/datatable';
import FieldType, { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';

import AggregationWizard from '../AggregationWizard';

const extendedTimeout = applyTimeoutMultiplier(15000);

jest.mock('views/hooks/useActiveQueryId');

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .visualizationConfig(DataTableVisualizationConfig.empty())
  .build();

const fieldType = new FieldType('field_type', ['numeric'], []);
const fieldTypeMapping1 = new FieldTypeMapping('took_ms', fieldType);
const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType);
const fieldTypeMapping3 = new FieldTypeMapping('timestamp', FieldTypes.DATE());
const fields = Immutable.List([fieldTypeMapping1, fieldTypeMapping2, fieldTypeMapping3]);
const fieldTypes = { all: fields, queryFields: Immutable.Map({ queryId: fields }) };

const plugin: PluginRegistration = { exports: { visualizationTypes: [dataTable] } };

const selectEventConfig = { container: document.body };

const addElement = async (key: 'Grouping' | 'Metric' | 'Sort') => {
  userEvent.click(await screen.findByRole('button', { name: 'Add' }));
  await screen.findByRole('menu');
  userEvent.click(await screen.findByRole('menuitem', { name: key }));
  await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
};

const selectField = async (fieldName: string, groupingIndex: number = 0, fieldSelectLabel = 'Add a field') => {
  const grouoingContainer = await screen.findByTestId(`grouping-${groupingIndex}`);
  const fieldSelection = within(grouoingContainer).getByLabelText(fieldSelectLabel);

  await act(async () => {
    await selectEvent.openMenu(fieldSelection);
    await selectEvent.select(fieldSelection, fieldName, selectEventConfig);
  });
};

const submitWidgetConfigForm = async () => {
  const applyButton = await screen.findByRole('button', { name: /update preview/i });
  fireEvent.click(applyButton);
};

const expectedPivotConfig = { skip_empty_values: undefined, limit: 15 };

describe('AggregationWizard', () => {
  type Props = Partial<React.ComponentProps<typeof AggregationWizard>> & {
    fieldTypesList?: {
      all: FieldTypeMappingsList
      queryFields: Map<string, FieldTypeMappingsList>,
    }
  }

  const renderSUT = ({ fieldTypesList = fieldTypes, ...props }: Props = {}) => render(
    <FieldTypesContext.Provider value={fieldTypesList}>
      <AggregationWizard config={widgetConfig}
                         editing
                         id="widget-id"
                         type="AGGREGATION"
                         onSubmit={() => {}}
                         onCancel={() => {}}
                         fields={Immutable.List([])}
                         onChange={() => {}}
                         {...props}>
        <span>The Visualization</span>
      </AggregationWizard>,
    </FieldTypesContext.Provider>,
  );

  beforeAll(() => PluginStore.register(plugin));

  afterAll(() => PluginStore.unregister(plugin));

  beforeEach(() => {
    asMock(useActiveQueryId).mockReturnValue('queryId');
  });

  it('should require group by function when adding a group by element', async () => {
    renderSUT();

    await addElement('Grouping');

    await screen.findByText('Field is required.');
  }, extendedTimeout);

  it('should add pivot to widget config', async () => {
    const onChange = jest.fn();
    renderSUT({ onChange });

    await addElement('Grouping');
    await selectField('took_ms');
    await submitWidgetConfigForm();

    const pivot = Pivot.createValues(['took_ms'], expectedPivotConfig);
    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should update config, even when field only exists for current query', async () => {
    const onChange = jest.fn();
    const queryFieldTypeMapping = new FieldTypeMapping('status_code', fieldType);
    const queryFields = Immutable.List([queryFieldTypeMapping]);
    renderSUT({ onChange, fieldTypesList: { all: fields, queryFields: Immutable.Map({ queryId: queryFields }) } });

    await addElement('Grouping');
    await selectField('status_code');
    await submitWidgetConfigForm();

    const pivot = Pivot.createValues(['status_code'], expectedPivotConfig);
    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should not throw an error when field in config no longer exists in field types list.', async () => {
    const onChange = jest.fn();
    const pivot = Pivot.createValues(['status_code']);
    const initialConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();

    renderSUT({
      onChange,
      fieldTypesList: { all: Immutable.List([]), queryFields: Immutable.Map({ queryId: Immutable.List([]) }) },
      config: initialConfig,
    });

    await screen.findByRole('button', { name: /update preview/i });
  }, extendedTimeout);

  it('should add multiple pivots to widget', async () => {
    const onChange = jest.fn();
    renderSUT({ onChange });

    await addElement('Grouping');
    await selectField('timestamp');
    await addElement('Grouping');
    await selectField('took_ms', 1);
    await submitWidgetConfigForm();

    const pivot0 = Pivot.create(['timestamp'], 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.createValues(['took_ms'], expectedPivotConfig);
    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot0, pivot1])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should add pivot with type "date" to widget when adding time field', async () => {
    const pivot = Pivot.create(['timestamp'], 'time', { interval: { type: 'timeunit', unit: 'minutes', value: 1 } });
    const onChange = jest.fn();
    renderSUT({ onChange });

    await addElement('Grouping');
    await selectField('timestamp');

    const autoCheckbox = await screen.findByRole('checkbox', { name: 'Auto' });
    await screen.findByRole('slider', { name: /interval/i });

    await userEvent.click(autoCheckbox);

    await screen.findByRole('button', { name: /minutes/i });

    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should add multiple fields to one pivot', async () => {
    const initialPivot = Pivot.createValues(['took_ms']);
    const updatedPivot = Pivot.createValues(['took_ms', 'http_method']);
    const config = widgetConfig
      .toBuilder()
      .rowPivots([initialPivot])
      .build();

    const onChange = jest.fn();
    renderSUT({ onChange, config });

    await screen.findByText('took_ms');
    await selectField('http_method', 0, 'Add another field');
    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([updatedPivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should save pivot with type "values" when adding date and values field', async () => {
    const initialPivot = Pivot.createValues(['took_ms']);
    const updatedPivot = Pivot.createValues(['took_ms', 'timestamp']);
    const config = widgetConfig
      .toBuilder()
      .rowPivots([initialPivot])
      .build();

    const onChange = jest.fn();
    renderSUT({ onChange, config });

    await screen.findByText('took_ms');
    await selectField('timestamp', 0, 'Add another field');
    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([updatedPivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should display limit field when all fields of a grouping have been removed', async () => {
    const pivot = Pivot.create(['timestamp'], 'time', { interval: { type: 'timeunit', unit: 'minutes', value: 1 } });
    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();
    renderSUT({ config });

    const deleteFieldButton = await screen.findByRole('button', { name: /remove timestamp field/i });

    userEvent.click(deleteFieldButton);

    await screen.findByLabelText('Limit');
  }, extendedTimeout);

  it('should display groupings with values from config', async () => {
    const pivot0 = Pivot.create(['timestamp'], 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.createValues(['took_ms']);
    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot0, pivot1])
      .build();

    renderSUT({ config });

    await screen.findByText('took_ms');
    await screen.findByText('timestamp');
  }, extendedTimeout);

  it('should remove all groupings', async () => {
    const pivot = Pivot.createValues(['took_ms']);
    const config = widgetConfig
      .toBuilder()
      .rowPivots([pivot])
      .build();
    const onChangeMock = jest.fn();
    renderSUT({ config, onChange: onChangeMock });

    const removeGroupingElementButton = screen.getByRole('button', { name: 'Remove Grouping' });
    userEvent.click(removeGroupingElementButton);

    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([])
      .build();

    await waitFor(() => expect(onChangeMock).toHaveBeenCalledTimes(1));

    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should display group by section even if config has no pivots', async () => {
    const config = widgetConfig
      .toBuilder()
      .build();

    renderSUT({ config });

    const configureElementsSection = await screen.findByTestId('configure-elements-section');

    expect(within(configureElementsSection).getByText('Group By')).toBeInTheDocument();
  }, extendedTimeout);

  it('should correctly update sort of groupings', async () => {
    const pivot0 = Pivot.create(['timestamp'], 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.createValues(['took_ms']);
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

    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([pivot1, pivot0])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);

  it('should correctly update sort of grouping fields', async () => {
    const initialPivot = Pivot.createValues(['http_method', 'took_ms']);
    const updatedPivot = Pivot.createValues(['took_ms', 'http_method']);
    const config = widgetConfig
      .toBuilder()
      .rowPivots([initialPivot])
      .build();

    const onChange = jest.fn();
    renderSUT({ onChange, config });

    const groupBySection = await screen.findByTestId('Group By-section');

    const firstItem = within(groupBySection).getByTestId('grouping-0-field-0-drag-handle');
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have lifted an item/i);
    fireEvent.keyDown(firstItem, { key: 'ArrowDown', keyCode: 40 });
    await screen.findByText(/You have moved the item/i);
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have dropped the item/i);

    await submitWidgetConfigForm();

    const updatedConfig = widgetConfig
      .toBuilder()
      .rowPivots([updatedPivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  }, extendedTimeout);
});
