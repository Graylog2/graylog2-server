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
import { simpleFields, simpleQueryFields } from 'fixtures/fields';

import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

import AggregationWizard from './AggregationWizard';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .build();

const fieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('aQueryId') };

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

  it('should display aggregation element as coming from config', async () => {
    const sort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending);
    const rowPivot = Pivot.create('timestamp', 'time', { interval: { type: 'timeunit', unit: 'minutes' } });

    const config = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .series([new Series('count()')])
      .sort([sort])
      .rowPivots([rowPivot])
      .build();

    renderSUT({ config });
    const configureElementsSection = await screen.findByTestId('configure-elements-section');

    const configuredElements = [
      'Metric',
    ];

    configuredElements.forEach((elementTitle) => {
      expect(within(configureElementsSection).getByText(elementTitle)).toBeInTheDocument();
    });
  });
});
