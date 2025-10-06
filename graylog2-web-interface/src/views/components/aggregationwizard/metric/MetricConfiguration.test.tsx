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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';
import { Formik } from 'formik';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';
import { act } from 'wrappedTestingLibrary/hooks';

import TestStoreProvider from 'views/test/TestStoreProvider';
import { SimpleFieldTypesContextProvider } from 'views/components/contexts/TestFieldTypesContextProvider';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';
import useViewsPlugin from 'views/test/testViewsPlugin';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';

import MetricConfiguration from './MetricConfiguration';

jest.mock('views/components/aggregationwizard/metric/ThresholdFormItem', () => ({ thresholdIndex, metricIndex }) => (
  <div data-testid={`threshold-item-${metricIndex}-${thresholdIndex}`}>Threshold {thresholdIndex}</div>
));

const fieldTypes = [FieldTypeMapping.create('field-allowed-0', FieldType.create('number', [Properties.Numeric]))];

const renderComponent = (
  props: {
    index: number;
  },
  formValues = {},
) =>
  render(
    <GraylogThemeProvider userIsLoggedIn>
      <TestStoreProvider>
        <SimpleFieldTypesContextProvider fields={fieldTypes}>
          <DefaultQueryClientProvider>
            <Formik initialValues={formValues} onSubmit={() => {}}>
              <MetricConfiguration {...props} />
            </Formik>
          </DefaultQueryClientProvider>
        </SimpleFieldTypesContextProvider>
      </TestStoreProvider>
    </GraylogThemeProvider>,
  );

describe('MetricConfiguration', () => {
  useViewsPlugin();

  const baseProps = {
    index: 0,
    currentFunction: 'count',
    currentMetric: {
      showThresholds: false,
      thresholds: [],
    },
    fields: [],
    onRemove: jest.fn(),
    fieldTypes: {},
    isOnlyMetric: false,
    isDrillDown: false,
  };

  it('shows the "Show line thresholds" checkbox', async () => {
    renderComponent(baseProps, { metrics: [baseProps.currentMetric], visualization: { type: 'bar' } });
    await screen.findByRole('checkbox', { name: /show line thresholds/i });
  });

  it('checks and initializes a threshold when checkbox is toggled on', async () => {
    renderComponent(baseProps, { metrics: [baseProps.currentMetric], visualization: { type: 'bar' } });

    const checkbox = await screen.findByRole('checkbox', { name: /show line thresholds/i });
    act(() => {
      fireEvent.click(checkbox);
    });

    await screen.findByTestId('threshold-item-0-0');
  });

  it('shows the add threshold button when showThresholds is true', async () => {
    const metricWithThresholds = {
      ...baseProps.currentMetric,
      showThresholds: true,
      thresholds: [{ color: '#fff', name: '', value: 0 }],
    };
    renderComponent(baseProps, { metrics: [metricWithThresholds], visualization: { type: 'bar' } });

    await screen.findByRole('button', { name: /add a threshold/i });
  });

  it('adds a new threshold when the add button is clicked', async () => {
    const metricWithThresholds = {
      ...baseProps.currentMetric,
      showThresholds: true,
      thresholds: [{ color: '#fff', name: '', value: 0 }],
    };
    renderComponent(baseProps, { metrics: [metricWithThresholds], visualization: { type: 'bar' } });

    const addButton = await screen.findByRole('button', { name: /add a threshold/i });
    fireEvent.click(addButton);

    // Should now render two threshold items
    await screen.findByTestId('threshold-item-0-0');
    await screen.findByTestId('threshold-item-0-1');
  });

  it('does not show thresholds UI for unsupported visualization types', () => {
    renderComponent(baseProps, { metrics: [baseProps.currentMetric], visualization: { type: 'pie' } });

    expect(screen.queryByRole('button', { name: /add a threshold/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId('threshold-item-0-0')).not.toBeInTheDocument();
  });
});
