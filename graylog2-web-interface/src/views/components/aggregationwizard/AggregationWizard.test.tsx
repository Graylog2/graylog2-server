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

  it('should list not configured aggregation actions in action select', async () => {
    const config = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .build();

    renderSUT({ config });
    const addActionSection = screen.getByTestId('add-action-section');

    const notConfiguredActions = [
      'Metric',
      'Group By',
      'Sort',
    ];

    notConfiguredActions.forEach((actionTitle) => {
      expect(within(addActionSection).getByText(actionTitle)).toBeInTheDocument();
    });

    expect(within(addActionSection).queryByText('Visualization')).not.toBeInTheDocument();
  });

  it('should display newly selected aggregation action', async () => {
    renderSUT();

    const aggregationActionSelect = screen.getByLabelText('Add an Action');
    const configureActionsSection = screen.getByTestId('configure-actions-section');

    expect(within(configureActionsSection).queryByText('Metric')).not.toBeInTheDocument();

    await selectEvent.openMenu(aggregationActionSelect);
    await selectEvent.select(aggregationActionSelect, 'Metric');

    expect(within(configureActionsSection).getByText('Metric')).toBeInTheDocument();
  });

  it('should delete aggregation action', async () => {
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
    const configureActionsSection = screen.getByTestId('configure-actions-section');
    const deleteAllMetricsButton = screen.getByTitle('Remove Metric');

    fireEvent.click(deleteAllMetricsButton);

    expect(onChangeMock).toHaveBeenCalledTimes(1);
    expect(onChangeMock).toHaveBeenCalledWith(updatedConfig);

    await waitFor(() => expect(within(configureActionsSection).queryByText('Metric')).not.toBeInTheDocument());
  });

  it('should display aggregation action as coming from config', async () => {
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
    const configureActionsSection = screen.getByTestId('configure-actions-section');

    const configuredActions = [
      'Metric',
      'Group By',
      'Sort',
      'Visualization',
    ];

    configuredActions.forEach((actionTitle) => {
      expect(within(configureActionsSection).getByText(actionTitle)).toBeInTheDocument();
    });
  });
});
