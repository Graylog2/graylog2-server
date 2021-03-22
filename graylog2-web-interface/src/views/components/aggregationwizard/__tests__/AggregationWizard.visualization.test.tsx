import React from 'react';
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import { PluginRegistration, PluginStore } from 'graylog-web-plugin/plugin';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import AggregationWizard from 'views/components/aggregationwizard/AggregationWizard';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .visualization('table')
  .build();

const SimpleAggregationWizard = (props) => (
  <AggregationWizard config={widgetConfig} editing id="widget-id" type="AGGREGATION" fields={Immutable.List([])} onChange={() => {}} {...props} />
);

const dataTableVisualization = makeVisualization(() => <span>This is the chart.</span>, 'table');

type ExtraConfigSettings = {
  mode: 'onemode' | 'anothermode' | 'thirdmode',
  color?: 'red' | 'green' | 'blue',
  invert: boolean,
  factor: number,
}

interface ExtraConfigWidget extends VisualizationConfig {
  mode: 'onemode' | 'anothermode' | 'thirdmode',
  color?: 'red' | 'green' | 'blue',
  invert: boolean,
  factor: number,
}

const fromConfig = (config: ExtraConfigWidget): ExtraConfigSettings => ({ ...config });
const createVisualizationConfig = (config: ExtraConfigSettings) => ({ ...config }) as ExtraConfigWidget;
const toConfig = (config: ExtraConfigSettings): ExtraConfigWidget => createVisualizationConfig(config);

const visualizationPlugin: PluginRegistration = {
  exports: {
    visualizationTypes: [{
      type: 'table',
      component: dataTableVisualization,
      displayName: 'Data Table',
    }, {
      type: 'visualizationWithConfig',
      displayName: 'Extra Config Required',
      component: dataTableVisualization,
      config: {
        fromConfig,
        toConfig,
        fields: [{
          name: 'mode',
          title: 'Mode',
          type: 'select',
          options: ['onemode', 'anothermode', 'thirdmode'],
          required: false,
        }, {
          name: 'color',
          title: 'Favorite Color',
          type: 'select',
          options: ['red', ['Yellow', 'green'], 'blue'],
          required: true,
          isShown: (formValues: ExtraConfigSettings) => formValues.mode === 'anothermode',
        }, {
          name: 'invert',
          title: 'Invert',
          type: 'boolean',
        }, {
          name: 'factor',
          title: 'Important Factor',
          type: 'numeric',
          required: true,
        }],
      },
    }, {
      type: 'withoutConfig',
      component: dataTableVisualization,
      displayName: 'Without Config',
    }],
  },
};

const expectSubmitButtonToBeDisabled = async () => {
  const submitButton = await screen.findByRole('button', { name: 'Apply Changes' });

  expect(submitButton).toBeDisabled();
};

const selectOption = async (ariaLabel: string, option: string) => {
  const select = await screen.findByLabelText(ariaLabel);
  await selectEvent.openMenu(select);
  await selectEvent.select(select, option);
};

describe('AggregationWizard/Visualizations', () => {
  beforeAll(() => PluginStore.register(visualizationPlugin));

  afterAll(() => PluginStore.unregister(visualizationPlugin));

  it('shows visualization section if it is present', async () => {
    render(<SimpleAggregationWizard />);

    await screen.findByText('Visualization');
    await screen.findByText('Data Table');
  });

  it('allows changing to visualization type without required fields', async () => {
    const onChange = jest.fn();
    render(<SimpleAggregationWizard onChange={onChange} />);

    const visualizationSelect = await screen.findByLabelText('Select visualization type');

    await selectEvent.openMenu(visualizationSelect);
    await selectEvent.select(visualizationSelect, 'Without Config');

    userEvent.click(await screen.findByRole('button', { name: 'Apply Changes' }));

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ visualization: 'withoutConfig', visualizationConfig: undefined })));
  });

  it('performs proper validation for required fields', async () => {
    const onChange = jest.fn();
    render(<SimpleAggregationWizard onChange={onChange} />);

    await selectOption('Select visualization type', 'Extra Config Required');

    await expectSubmitButtonToBeDisabled();

    const factorInput = await screen.findByRole('spinbutton', { name: 'Important Factor' });

    fireEvent.change(factorInput, { target: { value: '10' } });

    expect(await screen.findByRole('button', { name: 'Apply Changes' })).not.toBeDisabled();

    await selectOption('Select Mode', 'anothermode');

    await expectSubmitButtonToBeDisabled();

    await selectOption('Select Favorite Color', 'Yellow');

    const submitButton = await screen.findByRole('button', { name: 'Apply Changes' });

    expect(submitButton).not.toBeDisabled();

    userEvent.click(submitButton);

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'visualizationWithConfig',
      visualizationConfig: {
        color: 'green',
        factor: 10,
        mode: 'anothermode',
      },
    })));
  });
});
