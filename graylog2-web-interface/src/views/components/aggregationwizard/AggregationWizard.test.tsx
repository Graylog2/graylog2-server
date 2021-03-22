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
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import { simpleFields, simpleQueryFields } from 'fixtures/fields';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

import AggregationWizard from './AggregationWizard';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .build();

jest.mock('views/stores/AggregationFunctionsStore', () => ({
  getInitialState: jest.fn(() => ({ count: { type: 'count' }, min: { type: 'min' }, max: { type: 'max' }, percentile: { type: 'percentile' } })),
  listen: jest.fn(),
}));

const fieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('queryId') };

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
});
