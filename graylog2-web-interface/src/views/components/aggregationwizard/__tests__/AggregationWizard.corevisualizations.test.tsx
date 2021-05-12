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
import * as React from 'react';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import bindings from 'views/components/visualizations/bindings';
import AggregationWizard from 'views/components/aggregationwizard/AggregationWizard';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import AreaVisualization from 'views/components/visualizations/area/AreaVisualization';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import FieldTypesContext, { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import Pivot from 'views/logic/aggregationbuilder/Pivot';

const plugin: PluginRegistration = { exports: { visualizationTypes: bindings } };

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization('table')
  .build();

const fieldTypes: FieldTypes = {
  all: Immutable.List([]),
  queryFields: Immutable.Map(),
};
const SimpleAggregationWizard = (props) => (
  <FieldTypesContext.Provider value={fieldTypes}>
    <AggregationWizard config={widgetConfig} editing id="widget-id" type="AGGREGATION" fields={Immutable.List([])} onChange={() => {}} {...props} />
  </FieldTypesContext.Provider>
);

const submitButton = async () => screen.findByText('Apply Changes');

const expectSubmitButtonToBeDisabled = async () => {
  expect(await submitButton()).toBeDisabled();
};

const expectSubmitButtonNotToBeDisabled = async () => {
  expect(await submitButton()).not.toBeDisabled();
};

const visualizationSelect = async () => screen.findByLabelText('Select visualization type');

const selectOption = async (ariaLabel: string, option: string) => {
  const select = await screen.findByLabelText(ariaLabel);
  await selectEvent.openMenu(select);
  await selectEvent.select(select, option);
};

describe('AggregationWizard/Core Visualizations', () => {
  beforeAll(() => PluginStore.register(plugin));

  afterAll(() => PluginStore.unregister(plugin));

  it('shows all visualization types', async () => {
    render(<SimpleAggregationWizard />);

    await selectEvent.openMenu(await visualizationSelect());

    await screen.findByText('Area Chart');
    await screen.findByText('Bar Chart');
    await screen.findAllByText('Data Table');
    await screen.findByText('Heatmap');
    await screen.findByText('Line Chart');
    await screen.findByText('Pie Chart');
    await screen.findByText('Scatter Plot');
    await screen.findByText('Single Number');
    await screen.findByText('World Map');
  });

  it('creates Area Chart config when all required fields are present', async () => {
    const onChange = jest.fn();
    const areaChartConfig = widgetConfig.toBuilder().series([Series.create('count')]).build();

    render(<SimpleAggregationWizard onChange={onChange} config={areaChartConfig} />);

    await selectOption('Select visualization type', 'Area Chart');

    await selectOption('Select Interpolation', 'step-after');
    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'area',
      visualizationConfig: expect.objectContaining({
        interpolation: 'step-after',
      }),
    })));
  });

  it('creates Bar Chart config when all required fields are present', async () => {
    const onChange = jest.fn();
    const barChartConfig = widgetConfig.toBuilder().series([Series.create('count')]).build();

    render(<SimpleAggregationWizard onChange={onChange} config={barChartConfig} />);

    await selectOption('Select visualization type', 'Bar Chart');

    await selectOption('Select Mode', 'Stack');
    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'bar',
      visualizationConfig: expect.objectContaining({
        barmode: 'stack',
      }),
    })));
  });

  it('creates Line Chart config when all required fields are present', async () => {
    const onChange = jest.fn();
    const lineChartConfig = widgetConfig.toBuilder().series([Series.create('count')]).build();

    render(<SimpleAggregationWizard onChange={onChange} config={lineChartConfig} />);

    await selectOption('Select visualization type', 'Line Chart');

    await selectOption('Select Interpolation', 'spline');
    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'line',
      visualizationConfig: expect.objectContaining({
        interpolation: 'spline',
      }),
    })));
  });

  it('creates Heatmap config when all required fields are present', async () => {
    const onChange = jest.fn();
    const heatMapConfig = widgetConfig.toBuilder()
      .rowPivots([Pivot.create('foo', 'values')])
      .columnPivots([Pivot.create('bar', 'values')])
      .series([Series.create('count')])
      .build();

    render(<SimpleAggregationWizard onChange={onChange} config={heatMapConfig} />);

    await selectOption('Select visualization type', 'Heatmap');

    const useSmallestAsDefault = await screen.findByRole('checkbox', { name: 'Use smallest as default' });
    userEvent.click(useSmallestAsDefault);

    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'heatmap',
      visualizationConfig: expect.objectContaining({
        autoScale: true,
        colorScale: 'Viridis',
        reverseScale: false,
        useSmallestAsDefault: true,
      }),
    })));
  });

  it('creates Single Number config when all required fields are present', async () => {
    const onChange = jest.fn();

    render(<SimpleAggregationWizard onChange={onChange} />);

    await selectOption('Select visualization type', 'Single Number');

    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await screen.findByRole('checkbox', { name: 'Trend' }));

    await expectSubmitButtonToBeDisabled();

    await selectOption('Select Trend Preference', 'Higher');

    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'numeric',
      visualizationConfig: expect.objectContaining({
        trend: true,
        trendPreference: 'HIGHER',
      }),
    })));
  });

  it('clears validation errors properly when switching visualization', async () => {
    const areaChart = widgetConfig.toBuilder()
      .visualization(AreaVisualization.type)
      .visualizationConfig(AreaVisualizationConfig.create('linear'))
      .build();
    const onChange = jest.fn();

    render(<SimpleAggregationWizard config={areaChart} onChange={onChange} />);

    await selectOption('Select visualization type', 'Data Table');

    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'table',
      visualizationConfig: expect.objectContaining({}),
    })));
  });

  describe('has visualization-specific validation for aggregation config', () => {
    it.each`
    visualization      | error
    ${'Area Chart'}    | ${'Area chart requires at least one metric'}
    ${'Bar Chart'}     | ${'Bar chart requires at least one metric'}
    ${'Heatmap'}       | ${'Heatmap requires at least two groupings. At least one metric must be configured.'}
    ${'Line Chart'}    | ${'Line chart requires at least one metric'}
    ${'Pie Chart'}     | ${'Pie chart requires at least one metric'}
    ${'Scatter Plot'}  | ${'Scatter plot requires at least one metric'}
  `('expects constraints for $visualization', async ({ visualization, error }: { visualization: string, error: string }) => {
      render(<SimpleAggregationWizard />);

      await selectOption('Select visualization type', visualization);

      await expectSubmitButtonToBeDisabled();

      await waitFor(() => screen.findByText(error));
    });
  });
});
