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
import mockConfigurationOverridesApi, {
  configurationOverridesResponse,
  sampleGlobalConfiguration,
} from 'components/datanode/configuration/__tests__/mockConfigurationOverridesApi';
import changeSetting from 'components/datanode/configuration/__tests__/changeConfigurationField';
import clusterBindings from 'components/cluster-configuration/bindings';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';
import RoutePaths from 'routing/Routes';
import useConfigurationOverrides from 'components/datanode/configuration/useConfigurationOverrides';
import { Link } from 'components/common';
import configurationOverridesApi from 'components/datanode/configuration/configurationOverridesApi';

import DataNodeConfigurationPage from './DataNodeConfigurationPage';

jest.mock('hooks/useCurrentUser');
jest.mock('routing/request');
jest.mock('components/datanode/hooks/useShowDatanodeMigration');

const settingsPath = RoutePaths.SYSTEM.CLUSTER.DATANODE_CONFIGURATION;
const restartAction = 'Restart Data Nodes to Apply Changes';
const clusterPlugin = new PluginManifest({}, clusterBindings);

const SavedQueryCache = () => {
  const { data } = useConfigurationOverrides();

  return <span>Saved query cache: {data?.overrides['indices.queries.cache.size']?.value}</span>;
};

const renderPage = (section = 'storage', children?: React.ReactNode) => {
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
    mockConfigurationOverridesApi([sampleGlobalConfiguration]);
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

  it('opens Storage and Capacity by default with section navigation', async () => {
    const { router } = renderPage('');
    const lowWatermark = await screen.findByLabelText(/^Stop allocating shards at \(/);

    expect(router.state.location.pathname).toBe(`${settingsPath}/storage`);
    expect(screen.getByRole('heading', { name: 'Data Node Configuration' })).toBeInTheDocument();
    const clusterTab = screen.getByRole('link', { name: 'Cluster Nodes' });
    const configurationTab = screen.getByRole('link', { name: 'Data Node Configuration' });
    expect(screen.getAllByRole('link').slice(0, 3)).toEqual([
      clusterTab,
      configurationTab,
      screen.getByRole('link', { name: 'Certificate Management' }),
    ]);
    expect(clusterTab).toHaveAttribute('href', RoutePaths.SYSTEM.CLUSTER.NODES);
    expect(configurationTab).toHaveAttribute('href', settingsPath);
    expect(configurationTab).toHaveClass('active');
    expect(clusterTab).not.toHaveClass('active');
    expect(screen.getByRole('navigation', { name: 'Data Node configuration sections' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Storage and Capacity' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('href', `${settingsPath}/search`);
    expect(screen.getByRole('link', { name: 'Indexing and Recovery' })).toHaveAttribute(
      'href',
      `${settingsPath}/indexing`,
    );
    expect(screen.getByRole('heading', { name: 'Storage and Capacity' })).toBeInTheDocument();
    expect(lowWatermark).toHaveValue(85);
    expect(lowWatermark).not.toBeDisabled();
    expect(screen.getByLabelText(/^Move shards away at \(/)).toHaveValue(90);
    expect(screen.getByLabelText(/^Protect indexes at \(/)).toHaveValue(95);
    expect(screen.getByLabelText('Shard limit per data node')).toHaveValue(1000);
    expect(screen.queryByLabelText('Maximum query clauses')).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^Indexing buffer size \(/)).not.toBeInTheDocument();
    expect(screen.queryByText('Restart required')).not.toBeInTheDocument();
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Configuration scope' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Set override' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('shows only the selected section and updates the active navigation link', async () => {
    const { router } = renderPage();
    await screen.findByLabelText(/^Stop allocating shards at \(/);
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));

    const clauses = await screen.findByLabelText('Maximum query clauses');
    expect(clauses).toHaveValue(32768);
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10);
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveAccessibleDescription(/requires restarting/);
    const restartBadge = screen.getByRole('note', { name: /requires restarting the Data Nodes/ });
    await userEvent.hover(restartBadge);
    expect(await screen.findByRole('tooltip')).toHaveTextContent(
      'Changing this setting requires restarting the Data Nodes for the change to take effect.',
    );
    await userEvent.unhover(restartBadge);
    expect(clauses).not.toHaveAccessibleDescription(/requires restarting/);
    expect(screen.getByRole('heading', { name: 'Search' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Storage and Capacity' })).not.toHaveAttribute('aria-current');
    expect(screen.queryByLabelText(/^Stop allocating shards at \(/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^Indexing buffer size \(/)).not.toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`${settingsPath}/search`);

    await userEvent.click(screen.getByRole('link', { name: 'Indexing and Recovery' }));

    const buffer = await screen.findByLabelText(/^Indexing buffer size \(/);
    expect(buffer).toHaveValue(10);
    expect(screen.getByLabelText(/^Recovery bandwidth limit \(/)).toHaveValue(40);
    expect(screen.getByRole('heading', { name: 'Indexing and Recovery' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Maximum query clauses')).not.toBeInTheDocument();
    expect(screen.getAllByText('Restart required')).toHaveLength(1);
    expect(buffer).toHaveAccessibleName('Indexing buffer size (% of heap)');
    expect(buffer).toHaveAccessibleDescription(/requires restarting/);
    expect(screen.getByLabelText(/^Recovery bandwidth limit \(/)).not.toHaveAccessibleDescription(
      /requires restarting/,
    );
    expect(screen.queryByText(/Applies without restarting Data Nodes/)).not.toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`${settingsPath}/indexing`);
  });

  it('redirects unknown sections to the not-found page', async () => {
    renderPage('unknown');

    await screen.findByRole('heading', { name: 'Page not found' });
    expect(screen.queryByLabelText(/^Stop allocating shards at \(/)).not.toBeInTheDocument();
  });

  it('navigates between Cluster Nodes and the configuration tab', async () => {
    const { router } = renderPage('search');
    await screen.findByLabelText(/^Query cache size \(/);
    await userEvent.click(screen.getByRole('link', { name: 'Cluster Nodes' }));
    await screen.findByRole('heading', { name: 'Cluster Nodes' });
    expect(router.state.location.pathname).toBe(RoutePaths.SYSTEM.CLUSTER.NODES);
    expect(screen.getByRole('link', { name: 'Cluster Nodes' })).toHaveClass('active');
    expect(screen.getByRole('link', { name: 'Data Node Configuration' })).not.toHaveClass('active');
    await userEvent.click(screen.getByRole('link', { name: 'Data Node Configuration' }));
    await screen.findByLabelText(/^Stop allocating shards at \(/);
    expect(router.state.location.pathname).toBe(`${settingsPath}/storage`);
    expect(screen.getByRole('link', { name: 'Data Node Configuration' })).toHaveClass('active');
  });

  it('keeps unsaved configuration when switching Cluster tabs is declined', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const { router } = renderPage();
    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Cluster Nodes' }));
    expect(confirm).toHaveBeenCalled();
    expect(router.state.location.pathname).toBe(`${settingsPath}/storage`);
    expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(80);
  });

  it('shows percentage units in labels and accepts only numbers in the value field', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();
    const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
    expect(input).toHaveValue(85);
    await userEvent.clear(input);
    await userEvent.type(input, '88%');
    expect(input).toHaveValue(88);
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() =>
      expect(update).toHaveBeenCalledWith({ 'cluster.routing.allocation.disk.watermark.low': '88%' }, null),
    );
  });

  it('keeps the chosen bandwidth unit when clearing the number and restores it on cancel', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('indexing');
    const input = await screen.findByRole('spinbutton', { name: 'Recovery bandwidth limit (MB/s)' });
    expect(input).toHaveValue(40);
    await userEvent.click(screen.getByRole('button', { name: 'Recovery bandwidth limit unit' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'GB/s' }));
    await userEvent.clear(input);
    expect(screen.getByRole('button', { name: 'Recovery bandwidth limit unit' })).toHaveTextContent('GB/s');
    await userEvent.type(input, '2.5');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(update).toHaveBeenCalledWith({ 'indices.recovery.max_bytes_per_sec': '2.5gb' }, null));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    await userEvent.click(screen.getByRole('button', { name: 'Recovery bandwidth limit unit' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'MB/s' }));
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByRole('spinbutton', { name: 'Recovery bandwidth limit (GB/s)' })).toHaveValue(2.5);
    expect(screen.getByRole('button', { name: 'Recovery bandwidth limit unit' })).toHaveTextContent('GB/s');
  });

  it('loads saved ratios without changing them and saves only edited fields', async () => {
    await configurationOverridesApi.update({
      'cluster.routing.allocation.disk.watermark.low': '0.85',
      'cluster.routing.allocation.disk.watermark.high': '0.9',
      'cluster.routing.allocation.disk.watermark.flood_stage': '0.95',
    });
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();
    expect(await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (ratio)' })).toHaveValue(0.85);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    await changeSetting('Shard limit per data node', '1200');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(update).toHaveBeenCalledWith({ 'cluster.max_shards_per_node': '1200' }, null));
    expect(
      (await configurationOverridesApi.get()).overrides['cluster.routing.allocation.disk.watermark.low'].value,
    ).toBe('0.85');
  });

  it('applies only changed dynamic settings as raw strings for all Data Nodes', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith({ 'cluster.routing.allocation.disk.watermark.low': '80%' }, null),
    );
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());

    const saved = await configurationOverridesApi.get();
    expect(saved.node_type).toBe('datanode');
    expect(saved.node_id).toBeNull();
    expect(saved.overrides['cluster.routing.allocation.disk.watermark.low'].value).toBe('80%');
    expect(saved.overrides['cluster.routing.allocation.disk.watermark.high'].value).toBe('90%');
    expect(saved.overrides['indices.queries.cache.size'].value).toBe('10%');
  });

  it('keeps the original draft baseline when saved configuration refreshes', async () => {
    const { queryClient } = renderPage('search', <SavedQueryCache />);
    await changeSetting('Maximum query clauses', '40000');
    await act(async () => {
      await configurationOverridesApi.update({ 'indices.queries.cache.size': '20%' });
      await queryClient.invalidateQueries();
    });
    await screen.findByText('Saved query cache: 20%');
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(10);
    expect(screen.getByRole('spinbutton', { name: 'Maximum query clauses' })).toHaveValue(40000);
    expect(screen.queryByRole('button', { name: restartAction })).not.toBeInTheDocument();
    const update = jest.spyOn(configurationOverridesApi, 'update');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(update).toHaveBeenCalledWith({ 'indices.query.bool.max_clause_count': '40000' }, null));
    await waitFor(() =>
      expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(20),
    );
    expect((await configurationOverridesApi.get()).overrides['indices.queries.cache.size'].value).toBe('20%');
  });

  it('accepts a fractional numeric value without requiring a leading zero', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');
    const input = await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' });
    await userEvent.clear(input);
    await userEvent.type(input, '.5');
    expect(input).toHaveValue(0.5);
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(update).toHaveBeenCalledWith({ 'indices.queries.cache.size': '0.5%' }, null));
  });

  it('saves static overrides and restores the saved values on cancel', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');

    await changeSetting('Query cache size', '15%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(update).toHaveBeenCalledWith({ 'indices.queries.cache.size': '15%' }, null));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15);
    expect(screen.queryByRole('button', { name: restartAction })).not.toBeInTheDocument();

    await changeSetting('Query cache size', '20%');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('saves a mixture of static and dynamic overrides in one patch', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('indexing');

    await changeSetting('Recovery bandwidth limit', '45mb');
    await changeSetting('Indexing buffer size', '12%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(
        {
          'indices.recovery.max_bytes_per_sec': '45mb',
          'indices.memory.index_buffer_size': '12%',
        },
        null,
      ),
    );
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
  });

  it('cancels all draft changes and restores the normal action without saving', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');

    await changeSetting('Maximum query clauses', '40000');
    await changeSetting('Query cache size', '15%');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText('Maximum query clauses')).toHaveValue(32768);
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: restartAction })).not.toBeInTheDocument();
    expect(update).not.toHaveBeenCalled();
  });

  it.each([
    {
      section: 'storage',
      label: 'Stop allocating shards at',
      key: 'cluster.routing.allocation.disk.watermark.low',
      invalid: '101%',
      corrected: '84%',
      error: 'Enter a percentage from 0 to 100, a ratio from 0 to 1, or a non-negative free-space size.',
    },
    {
      section: 'search',
      label: 'Maximum query clauses',
      key: 'indices.query.bool.max_clause_count',
      invalid: '0',
      corrected: '40000',
      error: 'Enter a whole number greater than zero.',
    },
    {
      section: 'search',
      label: 'Query cache size',
      key: 'indices.queries.cache.size',
      invalid: '101%',
      corrected: '15%',
      error: 'Enter a percentage from 0 to 100 or a non-negative memory size.',
    },
    {
      section: 'indexing',
      label: 'Recovery bandwidth limit',
      key: 'indices.recovery.max_bytes_per_sec',
      invalid: '-1mb',
      corrected: '0mb',
      error: 'Enter a non-negative bandwidth limit. Use 0 to disable throttling.',
    },
  ])(
    'validates $label and allows applying after correction',
    async ({ section, label, key, invalid, corrected, error }) => {
      const update = jest.spyOn(configurationOverridesApi, 'update');
      renderPage(section);

      await changeSetting(label, invalid);
      await userEvent.click(getSaveButton());

      await screen.findByText(error);
      const invalidInput = screen.getByRole('spinbutton', { name: new RegExp(`^${label}`) });
      expect(invalidInput).toHaveAttribute('aria-invalid', 'true');
      expect(invalidInput).toHaveAccessibleDescription(new RegExp(escapeRegExp(error)));
      expect(update).not.toHaveBeenCalled();

      await changeSetting(label, corrected);
      await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
      await userEvent.click(getSaveButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith({ [key]: corrected }, null));
    },
  );

  it.each([
    {
      scenario: 'disk usage watermarks',
      low: '99%',
      high: '90%',
      flood: '95%',
      correctedLow: '80%',
      expectedPatch: { 'cluster.routing.allocation.disk.watermark.low': '80%' },
      error: 'Set watermarks in ascending order: low, high, then flood stage.',
    },
    {
      scenario: 'free-space watermarks',
      low: '5gb',
      high: '10gb',
      flood: '3gb',
      correctedLow: '20gb',
      expectedPatch: {
        'cluster.routing.allocation.disk.watermark.low': '20gb',
        'cluster.routing.allocation.disk.watermark.high': '10gb',
        'cluster.routing.allocation.disk.watermark.flood_stage': '3gb',
      },
      error: 'Set free-space watermarks in descending order: low, high, then flood stage.',
    },
  ])(
    'validates the order of $scenario and applies corrected values',
    async ({ low, high, flood, correctedLow, expectedPatch, error }) => {
      const update = jest.spyOn(configurationOverridesApi, 'update');
      renderPage();

      await changeSetting('Stop allocating shards at', low);
      await changeSetting('Move shards away at', high);
      await changeSetting('Protect indexes at', flood);
      await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

      await screen.findByText(error);
      expect(update).not.toHaveBeenCalled();

      await changeSetting('Stop allocating shards at', correctedLow);
      await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
      await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

      await waitFor(() => expect(update).toHaveBeenCalledWith(expectedPatch, null));
    },
  );

  it('rejects mixed watermark units and applies a consistent set of free-space values', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '20gb');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    const error = 'Use disk usage units for all three watermarks, or use free-space units for all three.';
    await screen.findByText(error);
    expect(update).not.toHaveBeenCalled();

    await changeSetting('Move shards away at', '10gb');
    await changeSetting('Protect indexes at', '5gb');
    await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(update).toHaveBeenCalledWith(
        {
          'cluster.routing.allocation.disk.watermark.low': '20gb',
          'cluster.routing.allocation.disk.watermark.high': '10gb',
          'cluster.routing.allocation.disk.watermark.flood_stage': '5gb',
        },
        null,
      ),
    );
  });

  it('keeps the draft when saving changes fails', async () => {
    jest.spyOn(configurationOverridesApi, 'update').mockRejectedValueOnce(new Error('Applying is unavailable.'));
    renderPage('search');

    await changeSetting('Query cache size', '15%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await screen.findByText(/Your changes have been kept/);
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled();

    const saved = await configurationOverridesApi.get();
    expect(saved.overrides['indices.queries.cache.size'].value).toBe('10%');
  });

  it('keeps saved settings when applying a different section', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();
    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());

    await userEvent.click(screen.getByRole('link', { name: 'Search' }));
    await changeSetting('Maximum query clauses', '40000');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());

    expect(update).toHaveBeenNthCalledWith(2, { 'indices.query.bool.max_clause_count': '40000' }, null);
    const saved = await configurationOverridesApi.get();
    expect(saved.overrides['cluster.routing.allocation.disk.watermark.low'].value).toBe('80%');
    expect(saved.overrides['indices.query.bool.max_clause_count'].value).toBe('40000');
    expect(saved.overrides['indices.queries.cache.size'].value).toBe('10%');

    await userEvent.click(screen.getByRole('link', { name: 'Storage and Capacity' }));
    const input = await screen.findByLabelText(/^Stop allocating shards at \(/);
    expect(input).toHaveValue(80);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('validates only the active section when another section has invalid saved values', async () => {
    await configurationOverridesApi.update({ 'cluster.routing.allocation.disk.watermark.low': '99%' });
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');

    await changeSetting('Maximum query clauses', '40000');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(update).toHaveBeenCalledWith({ 'indices.query.bool.max_clause_count': '40000' }, null));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    const saved = await configurationOverridesApi.get();
    expect(saved.overrides['cluster.routing.allocation.disk.watermark.low'].value).toBe('99%');
  });

  it('preserves the active section and draft when switching sections is declined', async () => {
    jest.spyOn(window, 'confirm').mockReturnValue(false);
    const { router } = renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));

    expect(router.state.location.pathname).toBe(`${settingsPath}/storage`);
    expect(screen.getByRole('link', { name: 'Storage and Capacity' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(80);
    expect(screen.queryByLabelText('Maximum query clauses')).not.toBeInTheDocument();
  });

  it('discards a draft when switching sections is confirmed', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(true);
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));
    await screen.findByLabelText('Maximum query clauses');
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    await userEvent.click(screen.getByRole('link', { name: 'Storage and Capacity' }));

    const input = await screen.findByLabelText(/^Stop allocating shards at \(/);
    expect(input).toHaveValue(85);
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(update).not.toHaveBeenCalled();
  });

  it('preserves a dirty draft when leaving the page is declined', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const { router } = renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Leave configuration' }));

    expect(confirm).toHaveBeenCalled();
    expect(router.state.location.pathname).toBe(`${settingsPath}/storage`);
    expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(80);
  });

  it('discards a dirty draft when leaving the page is confirmed', async () => {
    jest.spyOn(window, 'confirm').mockReturnValue(true);
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();

    await changeSetting('Stop allocating shards at', '80%');
    await userEvent.click(screen.getByRole('link', { name: 'Leave configuration' }));
    await screen.findByRole('heading', { name: 'Another page' });
    await userEvent.click(screen.getByRole('link', { name: 'Return to configuration' }));

    const input = await screen.findByLabelText(/^Stop allocating shards at \(/);
    expect(input).toHaveValue(85);
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
  it('loads an empty real scope without sample defaults and saves only an edited key', async () => {
    const transport = mockConfigurationOverridesApi();
    const notify = jest.spyOn(UserNotification, 'success');
    renderPage('search');
    expect(await screen.findByLabelText('Maximum query clauses')).toHaveValue(null);
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(null);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    expect(transport).toHaveBeenCalledWith(
      'GET',
      '/system/configuration_overrides',
      null,
      { node_type: 'datanode', node_id: undefined },
      { Accept: ['application/json'] },
      undefined,
    );
    await changeSetting('Maximum query clauses', '40000');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    expect(transport).toHaveBeenCalledWith(
      'PUT',
      '/system/configuration_overrides',
      { node_type: 'datanode', node_id: null, overrides: { 'indices.query.bool.max_clause_count': '40000' } },
      {},
      { Accept: ['application/json'] },
    );
    expect(notify).toHaveBeenCalledWith('Configuration overrides saved.');
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(null);
  });

  it('removes a real global override using null and preserves unrecognized keys', async () => {
    const transport = mockConfigurationOverridesApi([
      configurationOverridesResponse({ 'indices.queries.cache.size': '12%', 'other.setting': 'keep' }),
    ]);
    const { router } = renderPage('search');
    expect(await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(12);
    await userEvent.click(screen.getByRole('button', { name: /^Remove override/ }));
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(null);
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/configuration_overrides',
      { node_type: 'datanode', node_id: null, overrides: { 'indices.queries.cache.size': null } },
      {},
      { Accept: ['application/json'] },
    );
    const response = await transport.mock.results.at(-1).value;
    expect(response.overrides['other.setting'].value).toBe('keep');
    await userEvent.click(screen.getByRole('link', { name: 'Indexing and Recovery' }));
    expect(router.state.location.search).toBe('');
    expect(await screen.findByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('requires deletion permission for saved global overrides', async () => {
    mockConfigurationOverridesApi([configurationOverridesResponse({ 'indices.queries.cache.size': '12%' })]);
    asMock(useCurrentUser).mockReturnValue(
      defaultUser
        .toBuilder()
        .permissions(['clusterconfigentry:read', 'clusterconfigentry:create', 'clusterconfigentry:edit'])
        .build(),
    );
    renderPage('search');
    expect(await screen.findByRole('button', { name: /^Remove override/ })).toBeDisabled();
    await changeSetting('Maximum query clauses', '40000');
    expect(screen.getAllByRole('button', { name: /^Remove override/ })[0]).toBeEnabled();
  });

  it('keeps a real failed-save draft and supports cancel', async () => {
    const transport = mockConfigurationOverridesApi();
    renderPage('indexing');
    await changeSetting('Indexing buffer size', '15%');
    expect(screen.queryByRole('button', { name: restartAction })).not.toBeInTheDocument();
    transport.mockRejectedValueOnce(new Error('Save failed'));
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await screen.findByText(/Your changes have been kept/);
    expect(screen.getByRole('spinbutton', { name: 'Indexing buffer size (% of heap)' })).toHaveValue(15);
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByRole('spinbutton', { name: 'Indexing buffer size (% of heap)' })).toHaveValue(null);
  });

  it('shows a real loading error without falling back to samples', async () => {
    const transport = mockConfigurationOverridesApi();
    transport.mockRejectedValueOnce(new Error('Unavailable'));
    renderPage('search');
    await screen.findByText('Could not load Data Node configuration. Please try again.');
    expect(screen.queryByLabelText('Maximum query clauses')).not.toBeInTheDocument();
  });
});
