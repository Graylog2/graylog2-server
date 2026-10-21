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
import 'whatwg-fetch';
import React from 'react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { act, renderUnwrapped, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import DefaultProviders from 'DefaultProviders';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import escapeRegExp from 'lodash/escapeRegExp';
import { defaultUser } from 'defaultMockValues';
import { dataRouterFuture, routerProviderFuture } from 'reactRouterFutureFlags';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import AppConfig from 'util/AppConfig';
import UserNotification from 'util/UserNotification';
import mockManagedConfigurationApi, {
  managedConfigurationResponse,
  sampleClusterConfiguration,
} from 'components/datanode/configuration/__tests__/mockManagedConfigurationApi';
import changeSetting from 'components/datanode/configuration/__tests__/changeConfigurationField';
import clusterBindings from 'components/cluster-configuration/bindings';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';
import RoutePaths from 'routing/Routes';
import useManagedConfiguration from 'components/datanode/configuration/useManagedConfiguration';
import { Link } from 'components/common';
import managedConfigurationApi from 'components/datanode/configuration/managedConfigurationApi';

import DataNodeConfigurationPage from './DataNodeConfigurationPage';

jest.mock('hooks/useCurrentUser');
jest.mock('routing/request');
jest.mock('components/datanode/hooks/useShowDatanodeMigration');

const settingsPath = RoutePaths.SYSTEM.CLUSTER.DATANODE_CONFIGURATION;
const storageSection = 'storage-and-capacity';
const lowWatermarkName = 'cluster.routing.allocation.disk.watermark.low';
const shardLimitName = 'cluster.max_shards_per_node';
const clusterPlugin = new PluginManifest({}, clusterBindings);

const savedValueOf = async (name: string, nodeId: string | null = null) => {
  const configuration = await managedConfigurationApi.get(nodeId);

  return configuration.parameters.find((parameter) => parameter.name === name)?.value;
};

const SavedShardLimit = () => {
  const { data } = useManagedConfiguration();
  const shardLimit = data?.parameters.find(({ name }) => name === shardLimitName);

  return <span>Saved shard limit: {shardLimit?.value}</span>;
};

const renderPage = (section = storageSection, children?: React.ReactNode) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter(
    [
      {
        path: `${settingsPath}/*`,
        element: (
          <>
            <DataNodeConfigurationPage />
            {children}
            <Link to="/test/away">Leave configuration</Link>
          </>
        ),
      },
      { path: '/notfound', element: <h1>Page not found</h1> },
      {
        path: RoutePaths.SYSTEM.CLUSTER.NODES,
        element: (
          <>
            <ClusterConfigurationPageNavigation />
            <h1>Cluster Nodes</h1>
          </>
        ),
      },
      {
        path: '/test/away',
        element: (
          <>
            <h1>Another page</h1>
            <Link to={settingsPath}>Return to configuration</Link>
          </>
        ),
      },
    ],
    {
      initialEntries: [section ? `${settingsPath}/${section}` : settingsPath],
      future: dataRouterFuture,
    },
  );

  renderUnwrapped(
    <QueryClientProvider client={queryClient}>
      <DefaultProviders env="test">
        <RouterProvider router={router} future={routerProviderFuture} />
      </DefaultProviders>
    </QueryClientProvider>,
  );

  return { router, queryClient };
};

const getSaveButton = () => screen.getByRole('button', { name: 'Save changes' });

describe('DataNodeConfigurationPage', () => {
  beforeEach(() => {
    mockManagedConfigurationApi([sampleClusterConfiguration]);
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(AppConfig.gl2DevMode).mockReturnValue(false);
    asMock(useShowDatanodeMigration).mockReturnValue({
      isDatanodeConfiguredAndUsed: true,
      showDatanodeMigration: false,
    });
    PluginStore.register(clusterPlugin);
  });

  afterEach(() => {
    PluginStore.unregister(clusterPlugin);
    jest.restoreAllMocks();
    jest.clearAllMocks();
  });

  it('opens the only cluster wide section by default with section navigation', async () => {
    const { router } = renderPage('');
    const lowWatermark = await screen.findByLabelText(/^Stop allocating shards at \(/);

    expect(router.state.location.pathname).toBe(`${settingsPath}/${storageSection}`);
    expect(screen.getByRole('heading', { name: 'Data Node Configuration' })).toBeInTheDocument();
    const clusterTab = screen.getByRole('link', { name: 'Cluster Nodes' });
    const configurationTab = screen.getByRole('link', { name: 'Data Node Configuration' });
    expect(clusterTab).toHaveAttribute('href', RoutePaths.SYSTEM.CLUSTER.NODES);
    expect(configurationTab).toHaveAttribute('href', settingsPath);
    expect(configurationTab).toHaveClass('active');
    expect(clusterTab).not.toHaveClass('active');
    expect(screen.getByRole('navigation', { name: 'Data Node configuration sections' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Storage and Capacity' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { name: 'Storage and Capacity' })).toBeInTheDocument();
    expect(lowWatermark).toHaveValue(85);
    expect(lowWatermark).not.toBeDisabled();
    expect(screen.getByLabelText(/^Move shards away at \(/)).toHaveValue(90);
    expect(screen.getByLabelText(/^Protect indexes at \(/)).toHaveValue(95);
    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1000);
    // Parameters set per node live on the Data Node page, not here
    expect(screen.queryByLabelText('Maximum query clauses')).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^OpenSearch JVM heap \(/)).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Node Settings' })).not.toBeInTheDocument();
    expect(screen.queryByText('Restart required')).not.toBeInTheDocument();
    expect(getSaveButton()).toBeDisabled();
  });

  it('redirects unknown sections to the not-found page', async () => {
    renderPage('unknown');

    await screen.findByRole('heading', { name: 'Page not found' });
    expect(screen.queryByLabelText(/^Stop allocating shards at \(/)).not.toBeInTheDocument();
  });

  it('navigates between Cluster Nodes and the configuration tab', async () => {
    const { router } = renderPage();
    await screen.findByLabelText(/^Stop allocating shards at \(/);
    await userEvent.click(screen.getByRole('link', { name: 'Cluster Nodes' }));
    await screen.findByRole('heading', { name: 'Cluster Nodes' });
    expect(router.state.location.pathname).toBe(RoutePaths.SYSTEM.CLUSTER.NODES);
    expect(screen.getByRole('link', { name: 'Cluster Nodes' })).toHaveClass('active');
    expect(screen.getByRole('link', { name: 'Data Node Configuration' })).not.toHaveClass('active');
    await userEvent.click(screen.getByRole('link', { name: 'Data Node Configuration' }));
    await screen.findByLabelText(/^Stop allocating shards at \(/);
    expect(router.state.location.pathname).toBe(`${settingsPath}/${storageSection}`);
    expect(screen.getByRole('link', { name: 'Data Node Configuration' })).toHaveClass('active');
  });

  it('keeps unsaved configuration when switching Cluster tabs is declined', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const { router } = renderPage();
    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Cluster Nodes' }));
    expect(confirm).toHaveBeenCalled();
    expect(router.state.location.pathname).toBe(`${settingsPath}/${storageSection}`);
    expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(80);
  });

  it('shows percentage units in labels and accepts only numbers in the value field', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();
    const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
    expect(input).toHaveValue(85);
    await userEvent.clear(input);
    await userEvent.type(input, '88%');
    expect(input).toHaveValue(88);
    await userEvent.click(getSaveButton());
    await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '88%' }, null));
  });

  it('keeps the chosen unit when clearing the number and restores the saved value on cancel', async () => {
    renderPage();
    const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
    expect(input).toHaveValue(85);
    await userEvent.click(screen.getByRole('button', { name: 'Stop allocating shards at unit' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'GB free' }));
    await userEvent.clear(input);
    expect(screen.getByRole('button', { name: 'Stop allocating shards at unit' })).toHaveTextContent('GB free');
    await userEvent.type(input, '20');
    expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (GB free)' })).toHaveValue(20);

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(85);
    expect(getSaveButton()).toBeDisabled();
  });

  it('loads saved ratios without changing them and saves only edited fields', async () => {
    await managedConfigurationApi.update({
      [lowWatermarkName]: '0.85',
      'cluster.routing.allocation.disk.watermark.high': '0.9',
      'cluster.routing.allocation.disk.watermark.flood_stage': '0.95',
    });
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    expect(await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (Ratio (0–1))' })).toHaveValue(0.85);
    expect(getSaveButton()).toBeDisabled();

    await changeSetting('Shard limit per Data Node', '1200');
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null));
    expect(await savedValueOf(lowWatermarkName)).toBe('0.85');
  });

  it('saves only the changed settings as raw strings for all Data Nodes', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%' }, null));
    await waitFor(() => expect(getSaveButton()).toBeDisabled());

    const saved = await managedConfigurationApi.get();
    expect(saved.node_type).toBe('datanode');
    expect(saved.node_id).toBeNull();
    expect(saved.parameters.find(({ name }) => name === lowWatermarkName).value).toBe('80%');
    expect(saved.parameters.find(({ name }) => name === lowWatermarkName).source).toBe('database');
    expect(saved.parameters.find(({ name }) => name === shardLimitName).value).toBe('1000');
  });

  it('keeps the original draft baseline when saved configuration refreshes', async () => {
    const { queryClient } = renderPage(storageSection, <SavedShardLimit />);
    await changeSetting('Stop allocating shards at', '80%');

    await act(async () => {
      await managedConfigurationApi.update({ [shardLimitName]: '1500' });
      await queryClient.invalidateQueries();
    });

    await screen.findByText('Saved shard limit: 1500');
    expect(screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' })).toHaveValue(1000);
    expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(80);

    const update = jest.spyOn(managedConfigurationApi, 'update');
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%' }, null));
  });

  it('accepts a fractional numeric value without requiring a leading zero', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();
    const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
    await userEvent.clear(input);
    await userEvent.type(input, '.5');
    expect(input).toHaveValue(0.5);
    await userEvent.click(getSaveButton());
    await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '0.5%' }, null));
  });

  it('saves changes and restores the saved values on cancel', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('Shard limit per Data Node', '1200');
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null));
    await waitFor(() => expect(getSaveButton()).toBeDisabled());
    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);

    await changeSetting('Shard limit per Data Node', '1300');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);
    expect(getSaveButton()).toBeDisabled();
  });

  it('saves several changed settings in one patch', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await changeSetting('Shard limit per Data Node', '1200');
    await userEvent.click(getSaveButton());

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%', [shardLimitName]: '1200' }, null),
    );
    await waitFor(() => expect(getSaveButton()).toBeDisabled());
  });

  it('cancels all draft changes without saving', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await changeSetting('Shard limit per Data Node', '1200');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(85);
    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1000);
    expect(getSaveButton()).toBeDisabled();
    expect(update).not.toHaveBeenCalled();
  });

  it('validates the shard limit against the minimum the API provides', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    const error = 'Enter a number of at least 1.';
    renderPage();

    await changeSetting('Shard limit per Data Node', '0');
    await userEvent.click(getSaveButton());

    await screen.findByText(error);
    const invalidInput = screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' });
    expect(invalidInput).toHaveAttribute('aria-invalid', 'true');
    expect(invalidInput).toHaveAccessibleDescription(new RegExp(escapeRegExp(error)));
    expect(update).not.toHaveBeenCalled();

    await changeSetting('Shard limit per Data Node', '1200');
    await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null));
  });

  it('rejects a quantity without an amount against the pattern the API provides', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    const error = 'Enter a value in a supported format.';
    renderPage();

    const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
    await userEvent.clear(input);
    await userEvent.click(getSaveButton());

    await screen.findByText(error);
    expect(update).not.toHaveBeenCalled();

    await userEvent.type(input, '80');
    await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%' }, null));
  });

  it.each([
    {
      scenario: 'disk usage watermarks',
      low: '99%',
      high: '90%',
      flood: '95%',
      correctedLow: '80%',
      expectedPatch: { [lowWatermarkName]: '80%' },
      error: 'Set watermarks in ascending order: low, high, then flood stage.',
    },
    {
      scenario: 'free-space watermarks',
      low: '5gb',
      high: '10gb',
      flood: '3gb',
      correctedLow: '20gb',
      expectedPatch: {
        [lowWatermarkName]: '20gb',
        'cluster.routing.allocation.disk.watermark.high': '10gb',
        'cluster.routing.allocation.disk.watermark.flood_stage': '3gb',
      },
      error: 'Set free-space watermarks in descending order: low, high, then flood stage.',
    },
  ])(
    'validates the order of $scenario and applies corrected values',
    async ({ low, high, flood, correctedLow, expectedPatch, error }) => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeSetting('Stop allocating shards at', low);
      await changeSetting('Move shards away at', high);
      await changeSetting('Protect indexes at', flood);
      await userEvent.click(getSaveButton());

      await screen.findByText(error);
      expect(update).not.toHaveBeenCalled();

      await changeSetting('Stop allocating shards at', correctedLow);
      await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
      await userEvent.click(getSaveButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith(expectedPatch, null));
    },
  );

  it('rejects mixed watermark units and applies a consistent set of free-space values', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '20gb');
    await userEvent.click(getSaveButton());

    const error = 'Use disk usage units for all three watermarks, or use free-space units for all three.';
    await screen.findByText(error);
    expect(update).not.toHaveBeenCalled();

    await changeSetting('Move shards away at', '10gb');
    await changeSetting('Protect indexes at', '5gb');
    await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
    await userEvent.click(getSaveButton());

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(
        {
          [lowWatermarkName]: '20gb',
          'cluster.routing.allocation.disk.watermark.high': '10gb',
          'cluster.routing.allocation.disk.watermark.flood_stage': '5gb',
        },
        null,
      ),
    );
  });

  it('keeps the draft when saving changes fails', async () => {
    jest.spyOn(managedConfigurationApi, 'update').mockRejectedValueOnce(new Error('Applying is unavailable.'));
    renderPage();

    await changeSetting('Shard limit per Data Node', '1200');
    await userEvent.click(getSaveButton());

    await screen.findByText(/Your changes have been kept/);
    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);
    expect(getSaveButton()).toBeEnabled();
    expect(await savedValueOf(shardLimitName)).toBe('1000');
  });

  it('preserves a dirty draft when leaving the page is declined', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const { router } = renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Leave configuration' }));

    expect(confirm).toHaveBeenCalled();
    expect(router.state.location.pathname).toBe(`${settingsPath}/${storageSection}`);
    expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(80);
  });

  it('discards a dirty draft when leaving the page is confirmed', async () => {
    jest.spyOn(window, 'confirm').mockReturnValue(true);
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Leave configuration' }));
    await screen.findByRole('heading', { name: 'Another page' });
    await userEvent.click(screen.getByRole('link', { name: 'Return to configuration' }));

    expect(await screen.findByLabelText(/^Stop allocating shards at \(/)).toHaveValue(85);
    expect(update).not.toHaveBeenCalled();
  });

  it('shows saved settings without apply actions for read-only users', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(['clusterconfigentry:read']).build());
    renderPage();

    const input = await screen.findByLabelText(/^Stop allocating shards at \(/);
    expect(input).toBeDisabled();
    expect(input).toHaveValue(85);
    expect(screen.getByRole('button', { name: 'Stop allocating shards at unit' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
  });

  it('loads an empty scope without sample defaults and saves only an edited parameter', async () => {
    const transport = mockManagedConfigurationApi();
    const notify = jest.spyOn(UserNotification, 'success');
    renderPage();

    expect(await screen.findByLabelText('Shard limit per Data Node')).toHaveValue(null);
    expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(null);
    expect(getSaveButton()).toBeDisabled();
    expect(transport).toHaveBeenCalledWith(
      'GET',
      '/system/managed_configuration',
      null,
      { node_type: 'datanode', node_id: undefined },
      { Accept: ['application/json'] },
      undefined,
    );

    await changeSetting('Shard limit per Data Node', '1200');
    await userEvent.click(getSaveButton());

    await waitFor(() => expect(getSaveButton()).toBeDisabled());
    expect(transport).toHaveBeenCalledWith(
      'PUT',
      '/system/managed_configuration',
      { node_type: 'datanode', node_id: null, values: { [shardLimitName]: '1200' } },
      {},
      { Accept: ['application/json'] },
    );
    expect(notify).toHaveBeenCalledWith('Configuration saved.');
  });

  it('removes a stored value by sending null', async () => {
    const transport = mockManagedConfigurationApi([managedConfigurationResponse({ [shardLimitName]: '1200' })]);
    renderPage();

    expect(await screen.findByLabelText('Shard limit per Data Node')).toHaveValue(1200);
    await userEvent.click(screen.getByRole('button', { name: 'Remove stored value for Shard limit per Data Node' }));
    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(null);

    await userEvent.click(getSaveButton());

    await waitFor(() => expect(getSaveButton()).toBeDisabled());
    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/managed_configuration',
      { node_type: 'datanode', node_id: null, values: { [shardLimitName]: null } },
      {},
      { Accept: ['application/json'] },
    );
    expect(await savedValueOf(shardLimitName)).toBeNull();
  });

  it('requires deletion permission to remove a stored value', async () => {
    mockManagedConfigurationApi([managedConfigurationResponse({ [shardLimitName]: '1200' })]);
    asMock(useCurrentUser).mockReturnValue(
      defaultUser
        .toBuilder()
        .permissions(['clusterconfigentry:read', 'clusterconfigentry:create', 'clusterconfigentry:edit'])
        .build(),
    );
    renderPage();

    expect(await screen.findByRole('button', { name: /^Remove stored value/ })).toBeDisabled();
    expect(screen.getByLabelText('Shard limit per Data Node')).not.toBeDisabled();
  });

  it('keeps a failed-save draft and supports cancel', async () => {
    const transport = mockManagedConfigurationApi();
    renderPage();

    await changeSetting('Shard limit per Data Node', '1200');
    transport.mockRejectedValueOnce(new Error('Save failed'));
    await userEvent.click(getSaveButton());

    await screen.findByText(/Your changes have been kept/);
    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(null);
  });

  it('shows a loading error without falling back to samples', async () => {
    const transport = mockManagedConfigurationApi();
    transport.mockRejectedValueOnce(new Error('Unavailable'));
    renderPage();

    await screen.findByText('Could not load Data Node configuration. Please try again.');
    expect(screen.queryByLabelText('Shard limit per Data Node')).not.toBeInTheDocument();
  });
});
