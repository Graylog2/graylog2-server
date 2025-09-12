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

// ThresholdFormItem.test.tsx
import React from 'react';
import { render, fireEvent, screen } from 'wrappedTestingLibrary';
import { Formik } from 'formik';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

import ThresholdFormItem from './ThresholdFormItem';

const defaultMetrics = [
  {
    field: 'duration',
    thresholds: [{ name: 'Critical', value: 100, color: '#ff0000' }],
  },
];

const defaultUnits = {
  duration: { abbrev: 's', unitType: 'time' },
};

const defaultInitialValues = {
  metrics: defaultMetrics,
  units: defaultUnits,
};

const renderComponent = (
  props: { metricIndex: number; thresholdIndex: number; onRemove: () => void },
  formValues = defaultInitialValues,
) =>
  render(
    <GraylogThemeProvider userIsLoggedIn>
      <Formik initialValues={formValues} onSubmit={() => {}}>
        <ThresholdFormItem {...props} />
      </Formik>
    </GraylogThemeProvider>,
  );

describe('ThresholdFormItem', () => {
  it('renders name and value fields', async () => {
    renderComponent({ metricIndex: 0, thresholdIndex: 0, onRemove: () => {} });
    await screen.findByLabelText(/Name/i);
    await screen.findByLabelText(/Value/i);
  });

  it('renders color hint and opens color picker popover', async () => {
    renderComponent({ metricIndex: 0, thresholdIndex: 0, onRemove: () => {} });
    const colorHint = await screen.findByLabelText(/Color Hint/i);
    fireEvent.click(colorHint);
    await screen.findByText(/Color configuration for threshold/i);
  });

  it('calls remove callback when delete button is clicked', async () => {
    const removeMock = jest.fn();
    renderComponent({ metricIndex: 0, thresholdIndex: 0, onRemove: removeMock });
    const deleteButton = await screen.findByTitle(/Remove threshold/i);
    fireEvent.click(deleteButton);

    expect(removeMock).toHaveBeenCalled();
  });

  it('shows correct help text for unit', async () => {
    renderComponent({ metricIndex: 0, thresholdIndex: 0, onRemove: () => {} });
    await screen.findByText(/Value is in seconds/i);
  });
});
