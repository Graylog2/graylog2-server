import React from 'react';
import * as Immutable from 'immutable';
import { act, fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import FieldType, { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

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

  it('should require group by function when adding a group by element', async () => {
    renderSUT();

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Group By');

    await waitFor(() => expect(screen.getByText('Field is required.')).toBeInTheDocument());
  });

  it('should change the config when applied', async () => {
    const onChange = jest.fn();
    renderSUT({ onChange });

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Group By');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'took_ms');
    });

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const pivot = Pivot.create('took_ms', 'values', { limit: 15 });
    const updatedConfig = AggregationWidgetConfig
      .builder()
      .rowPivots([pivot])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  });

  it('should handle timestamp field types', async () => {
    renderSUT();

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Group By');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'timestamp');
    });

    const autoCheckbox = await screen.findByRole('checkbox', { name: 'Auto' });
    await screen.findByText(/A smaller granularity leads to/);

    await userEvent.click(autoCheckbox);

    await screen.findByText('The size of the buckets for this timestamp type.');
  });

  it('should create group by with multiple groupings', async () => {
    const onChange = jest.fn();
    renderSUT({ onChange });

    const aggregationElementSelect = screen.getByLabelText('Select an element to add ...');

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Group By');

    const fieldSelection = await screen.findByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelection);
      await selectEvent.select(fieldSelection, 'timestamp');
    });

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Group By');

    const fieldSelections = await screen.findAllByLabelText('Field');

    await act(async () => {
      await selectEvent.openMenu(fieldSelections[1]);
      await selectEvent.select(fieldSelections[1], 'took_ms');
    });

    const applyButton = await screen.findByRole('button', { name: 'Apply Changes' });
    fireEvent.click(applyButton);

    const pivot0 = Pivot.create('timestamp', 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.create('took_ms', 'values', { limit: 15 });
    const updatedConfig = AggregationWidgetConfig
      .builder()
      .rowPivots([pivot0, pivot1])
      .build();

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(1));

    expect(onChange).toHaveBeenCalledWith(updatedConfig);
  });

  it('should display group by with values from config', async () => {
    const pivot0 = Pivot.create('timestamp', 'time', { interval: { type: 'auto', scaling: 1 } });
    const pivot1 = Pivot.create('took_ms', 'values', { limit: 15 });
    const config = AggregationWidgetConfig
      .builder()
      .rowPivots([pivot0, pivot1])
      .build();

    renderSUT({ config });

    await screen.findByText('took_ms');
    await screen.findByText('timestamp');
  });
});
