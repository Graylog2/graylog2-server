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
import userEvent from '@testing-library/user-event';

import { simpleFields, simpleQueryFields } from 'fixtures/fields';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import DataTable from 'views/components/datatable';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';

import AggregationWizard from './AggregationWizard';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization(DataTable.type)
  .build();

jest.mock('views/hooks/useAggregationFunctions');

const fieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('queryId') };

describe('AggregationWizard', () => {
  const renderSUT = (props: Partial<React.ComponentProps<typeof AggregationWizard>> = {}) => render((
    <TestStoreProvider>
      <FieldTypesContext.Provider value={fieldTypes}>
        <AggregationWizard onChange={() => {}}
                           onSubmit={() => {}}
                           onCancel={() => {}}
                           config={widgetConfig}
                           editing
                           id="widget-id"
                           type="AGGREGATION"
                           fields={Immutable.List([])}
                           {...props}>
          <div>The Visualization</div>
        </AggregationWizard>
      </FieldTypesContext.Provider>
    </TestStoreProvider>
  ));

  useViewsPlugin();

  it('should render visualization passed as children', async () => {
    renderSUT();

    expect(await screen.findByText('The Visualization')).toBeInTheDocument();
  });

  it('should list available aggregation elements in element select', async () => {
    const config = AggregationWidgetConfig
      .builder()
      .visualization(DataTable.type)
      .build();

    renderSUT({ config });

    await userEvent.click(await screen.findByRole('button', { name: 'Add' }));
    const addElementMenu = await screen.findByRole('menu');
    const notConfiguredElements = [
      'Metric',
      'Grouping',
      'Sort',
    ];

    notConfiguredElements.forEach((elementTitle) => {
      expect(within(addElementMenu).getByText(elementTitle)).toBeInTheDocument();
    });
  });

  it('should display newly selected aggregation element', async () => {
    renderSUT();

    const metricsSection = await screen.findByTestId('Metrics-section');

    expect(within(metricsSection).queryByText('Function')).not.toBeInTheDocument();

    await userEvent.click(await screen.findByRole('button', { name: 'Add' }));

    await userEvent.click(await screen.findByRole('menuitem', { name: 'Metric' }));

    await waitFor(() => within(metricsSection).findByText('Function'));
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('should call onSubmit', async () => {
    const onSubmit = jest.fn();
    renderSUT({ onSubmit });
    userEvent.click(await screen.findByRole('button', { name: /update widget/i }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });

  it('should call onCancel', async () => {
    const onCancel = jest.fn();
    renderSUT({ onCancel });
    userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    await waitFor(() => expect(onCancel).toHaveBeenCalledTimes(1));
  });
});
