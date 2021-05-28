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
const mapVisualization = makeVisualization(() => <span>This is the map.</span>, 'map');

interface ExtraConfigSettings {
  mode: 'onemode' | 'anothermode' | 'thirdmode',
  color?: 'red' | 'green' | 'blue',
  invert: boolean,
  factor: number,
}

interface ExtraConfigWidget extends VisualizationConfig, ExtraConfigSettings {}

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
      type: 'map',
      displayName: 'World Map',
      component: mapVisualization,
      config: {
        fromConfig,
        toConfig,
        fields: [],
      },
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

  it('should update visualization config when changing config inside visualization', async () => {
    const worldMapConfig = widgetConfig.toBuilder().visualization('map').build();
    const onChange = jest.fn();

    const WorldMap = ({ onVisualizationConfigChange }: { onVisualizationConfigChange?: (newViewport: { zoom: number, centerX: number, centerY: number }) => void }) => (
      <button type="button" onClick={() => onVisualizationConfigChange({ zoom: 2, centerX: 40, centerY: 50 })}>Change Viewport</button>
    );
    WorldMap.defaultProps = { onVisualizationConfigChange: undefined };

    render(
      <SimpleAggregationWizard onChange={onChange} config={worldMapConfig}>
        <WorldMap />
      </SimpleAggregationWizard>,
    );

    const updateViewportButton = await screen.findByRole('button', { name: 'Change Viewport' });
    userEvent.click(updateViewportButton);
    const submitButton = await screen.findByRole('button', { name: 'Apply Changes' });
    userEvent.click(submitButton);

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
      visualization: 'map',
      visualizationConfig: {
        zoom: 2,
        centerX: 40,
        centerY: 50,
      },
    })));
  });
});
