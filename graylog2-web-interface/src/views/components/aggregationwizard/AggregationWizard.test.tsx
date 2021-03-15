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
import { fireEvent, render, screen, waitFor, within } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable/DataTable';
import Direction from 'views/logic/aggregationbuilder/Direction';

import AggregationWizard from './AggregationWizard';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .build();

describe('AggregationWizard', () => {
  const renderSUT = (props = {}) => render(
    <AggregationWizard onChange={() => {}}
                       config={widgetConfig}
                       editing
                       id="widget-id"
                       type="AGGREGATION"
                       fields={Immutable.List([])}
                       {...props}>
      The Visualization
    </AggregationWizard>,
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
    const addElementSection = screen.getByTestId('add-element-section');
    const aggregationElementSelect = screen.getByLabelText('Add an Element');
    const notConfiguredElements = [
      'Metric',
      'Group By',
      'Sort',
    ];
    await selectEvent.openMenu(aggregationElementSelect);

    notConfiguredElements.forEach((elementTitle) => {
      expect(within(addElementSection).getByText(elementTitle)).toBeInTheDocument();
    });

    expect(within(addElementSection).queryByText('Visualization')).not.toBeInTheDocument();
  });

  it('should display newly selected aggregation element', async () => {
    renderSUT();

    const aggregationElementSelect = screen.getByLabelText('Add an Element');
    const configureElementsSection = screen.getByTestId('configure-elements-section');

    expect(within(configureElementsSection).queryByText('Metric')).not.toBeInTheDocument();

    await selectEvent.openMenu(aggregationElementSelect);
    await selectEvent.select(aggregationElementSelect, 'Metric');

    expect(within(configureElementsSection).getByText('Metric')).toBeInTheDocument();
  });

  it('should delete aggregation element', async () => {
    const onChangeMock = jest.fn();
    const config = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .series([new Series('count()')])
      .build();
    const updatedConfig = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .series([])
      .build();

    renderSUT({ config, onChange: onChangeMock });
    const configureElementsSection = screen.getByTestId('configure-elements-section');
    const deleteAllMetricsButton = screen.getByTitle('Remove Metric');

    fireEvent.click(deleteAllMetricsButton);

    expect(onChangeMock).toHaveBeenCalledTimes(1);
    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);

    await waitFor(() => expect(within(configureElementsSection).queryByText('Metric')).not.toBeInTheDocument());
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
    const configureElementsSection = screen.getByTestId('configure-elements-section');

    const configuredElements = [
      'Metric',
      'Group By',
      'Sort',
      'Visualization',
    ];

    configuredElements.forEach((elementTitle) => {
      expect(within(configureElementsSection).getByText(elementTitle)).toBeInTheDocument();
    });
  });
});
