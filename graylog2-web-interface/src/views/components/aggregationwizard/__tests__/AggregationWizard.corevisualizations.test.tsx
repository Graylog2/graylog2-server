import * as React from 'react';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import bindings from 'views/bindings';
import AggregationWizard from 'views/components/aggregationwizard/AggregationWizard';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

const plugin: PluginRegistration = { exports: bindings };

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization('table')
  .build();

const SimpleAggregationWizard = (props) => (
  <AggregationWizard config={widgetConfig} editing id="widget-id" type="AGGREGATION" fields={Immutable.List([])} onChange={() => {}} {...props} />
);

const submitButton = async () => screen.findByRole('button', { name: 'Apply Changes' });

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

  it.each`
    visualization      | fields
    ${'Area Chart'}    | ${['Interpolation']}
    ${'Bar Chart'}     | ${['Mode']}
    ${'Line Chart'}    | ${['Interpolation']}
    ${'Heatmap'}       | ${['Color Scale', 'Min', 'Max', 'Default Value']}
    ${'Pie Chart'}     | ${[]}
    ${'Scatter Plot'}  | ${[]}
    ${'Single Number'} | ${[]}
    ${'World Map'}     | ${[]}
  `('expects mandatory fields for $visualization', async ({ visualization, fields }: { visualization: string, fields: Array<string> }) => {
    render(<SimpleAggregationWizard />);

    await selectEvent.select(await visualizationSelect(), visualization);

    if (fields.length > 0) {
      await expectSubmitButtonToBeDisabled();
    } else {
      await expectSubmitButtonNotToBeDisabled();
    }

    // eslint-disable-next-line no-restricted-syntax
    for (const field of fields) {
      expect(screen.getByText(`${field} is required.`)).toBeInTheDocument();
    }
  });

  it('creates Area Chart config when all required fields are present', async () => {
    const onChange = jest.fn();

    render(<SimpleAggregationWizard onChange={onChange} />);

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

    render(<SimpleAggregationWizard onChange={onChange} />);

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

    render(<SimpleAggregationWizard onChange={onChange} />);

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

    render(<SimpleAggregationWizard onChange={onChange} />);

    await selectOption('Select visualization type', 'Heatmap');

    await expectSubmitButtonToBeDisabled();

    await selectOption('Select Color Scale', 'Viridis');

    const minInput = await screen.findByRole('spinbutton', { name: 'Min' });
    userEvent.type(minInput, '1');

    const maxInput = await screen.findByRole('spinbutton', { name: 'Max' });
    userEvent.type(maxInput, '100');

    const useSmallestAsDefault = await screen.findByRole('checkbox', { name: 'Use smallest as default' });
    userEvent.click(useSmallestAsDefault);

    await expectSubmitButtonNotToBeDisabled();

    userEvent.click(await submitButton());

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'heatmap',
      visualizationConfig: expect.objectContaining({
        autoScale: false,
        colorScale: 'Viridis',
        reverseScale: false,
        useSmallestAsDefault: true,
        zMin: 1,
        zMax: 100,
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
});
