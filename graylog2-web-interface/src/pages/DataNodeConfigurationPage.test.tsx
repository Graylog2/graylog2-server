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
import omitBy from 'lodash/omitBy';
import { defaultUser } from 'defaultMockValues';
import { dataRouterFuture, routerProviderFuture } from 'reactRouterFutureFlags';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import AppConfig from 'util/AppConfig';
import UserNotification from 'util/UserNotification';
import mockManagedConfigurationApi, {
  managedConfigurationResponse,
  configurationParameter,
  sampleClusterConfiguration,
} from 'components/datanode/configuration/testing/mockManagedConfigurationApi';
import changeSetting from 'components/datanode/configuration/testing/changeConfigurationField';
import clusterBindings from 'components/cluster-configuration/bindings';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';
import RoutePaths from 'routing/Routes';
import useManagedConfiguration from 'components/datanode/configuration/useManagedConfiguration';
import { Link } from 'components/common';
import managedConfigurationApi from 'components/datanode/configuration/managedConfigurationApi';
import FetchError from 'logic/errors/FetchError';

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

const getApplyButton = () => screen.getByRole('button', { name: /apply changes/i });

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

  describe('Rendering and navigation', () => {
    it('opens the only cluster wide section without redundant navigation', async () => {
      const { router } = renderPage('');
      const lowWatermark = await screen.findByLabelText(/^Stop allocating shards at \(/);

      expect(router.state.location.pathname).toBe(`${settingsPath}/${storageSection}`);
      expect(screen.getByRole('heading', { name: 'Data Node Configuration' })).toBeInTheDocument();
      expect(screen.queryByRole('navigation', { name: 'Data Node configuration sections' })).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Storage and Capacity' })).toBeInTheDocument();
      expect(lowWatermark).toHaveValue(85);
      expect(lowWatermark).not.toBeDisabled();
      expect(screen.getByLabelText(/^Move shards away at \(/)).toHaveValue(90);
      expect(screen.getByLabelText(/^Protect indexes at \(/)).toHaveValue(95);
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1000);
      expect(getApplyButton()).toBeDisabled();
      expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
    });

    it('adds section navigation when the backend supplies another category', async () => {
      jest.spyOn(managedConfigurationApi, 'get').mockResolvedValue({
        ...sampleClusterConfiguration,
        parameters: [
          ...sampleClusterConfiguration.parameters,
          configurationParameter({
            name: 'additional_setting',
            label: 'Additional setting',
            category: 'Additional Settings',
            scope: 'cluster',
            requires_restart: true,
          }),
        ],
      });
      renderPage();
      await screen.findByRole('navigation', { name: 'Data Node configuration sections' });
      expect(screen.getByRole('link', { name: 'Storage and Capacity' })).toHaveAttribute('aria-current', 'page');
      await changeSetting('Shard limit per Data Node', '1200');
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
      await userEvent.click(screen.getByRole('link', { name: 'Additional Settings' }));
      await userEvent.type(await screen.findByLabelText('Additional setting'), 'enabled');
      expect(getApplyButton()).toBeEnabled();
      await userEvent.click(screen.getByRole('link', { name: 'Storage and Capacity' }));
      expect(await screen.findByLabelText('Shard limit per Data Node')).toHaveValue(1200);
      const restartWarning = screen.getByRole('alert', { name: 'Restart required after saving' });
      expect(restartWarning).toHaveTextContent(
        'Changes to the following settings require restarting the Data Nodes to take effect:',
      );
      expect(restartWarning).not.toHaveTextContent('Shard limit per Data Node');
      await userEvent.click(screen.getByRole('link', { name: 'Additional setting' }));
      expect(await screen.findByRole('textbox', { name: 'Additional setting' })).toHaveFocus();
      expect(getApplyButton()).toBeEnabled();
      expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Nodes');
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
      await changeSetting('Stop allocating shards at', '80', '% disk used');
      await userEvent.click(screen.getByRole('link', { name: 'Cluster Nodes' }));
      expect(confirm).toHaveBeenCalled();
      expect(router.state.location.pathname).toBe(`${settingsPath}/${storageSection}`);
      expect(screen.getByLabelText(/^Stop allocating shards at \(/)).toHaveValue(80);
    });

    it('discards a dirty draft when leaving the page is confirmed', async () => {
      jest.spyOn(window, 'confirm').mockReturnValue(true);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeSetting('Stop allocating shards at', '80', '% disk used');
      await userEvent.click(screen.getByRole('link', { name: 'Leave configuration' }));
      await screen.findByRole('heading', { name: 'Another page' });
      await userEvent.click(screen.getByRole('link', { name: 'Return to configuration' }));

      expect(await screen.findByLabelText(/^Stop allocating shards at \(/)).toHaveValue(85);
      expect(update).not.toHaveBeenCalled();
    });
  });

  describe('Saving and drafts', () => {
    it('keeps the saved cluster restart warning when later edits are canceled', async () => {
      mockManagedConfigurationApi([
        {
          ...sampleClusterConfiguration,
          parameters: sampleClusterConfiguration.parameters.map((parameter) =>
            parameter.name === shardLimitName ? { ...parameter, requires_restart: true } : parameter,
          ),
        },
      ]);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await changeSetting('Shard limit per Data Node', '1200');
      await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

      const warning = await screen.findByRole('alert', { name: 'Saved changes require a restart' });
      expect(warning).toHaveTextContent(
        'The following settings have been saved. Restart the Data Nodes for them to take effect:',
      );
      expect(warning).toHaveTextContent('Shard limit per Data Node');
      expect(screen.getByRole('button', { name: 'Restart Data Nodes' })).toBeEnabled();

      await changeSetting('Stop allocating shards at', '80', '% disk used');
      expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled();
      await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(85);
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);
      expect(warning).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Restart Data Nodes' })).toBeEnabled();
      expect(update).toHaveBeenCalledTimes(1);
      expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null);
    });

    it('saves only the changed settings as raw strings for all Data Nodes', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeSetting('Stop allocating shards at', '80', '% disk used');
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%' }, null));
      await waitFor(() => expect(getApplyButton()).toBeDisabled());

      const saved = await managedConfigurationApi.get();
      expect(saved.node_type).toBe('datanode');
      expect(saved.node_id).toBeNull();
      expect(saved.parameters.find(({ name }) => name === lowWatermarkName).value).toBe('80%');
      expect(saved.parameters.find(({ name }) => name === lowWatermarkName).source).toBe('database');
      expect(saved.parameters.find(({ name }) => name === shardLimitName).value).toBe('1000');
    });

    it('keeps the draft baseline and hides mismatched sources when saved configuration refreshes', async () => {
      mockManagedConfigurationApi([
        {
          ...sampleClusterConfiguration,
          parameters: sampleClusterConfiguration.parameters.map((parameter) =>
            parameter.name === shardLimitName
              ? { ...parameter, node_value: '1500', node_source: 'config_file' }
              : parameter,
          ),
        },
      ]);
      const { queryClient } = renderPage(storageSection, <SavedShardLimit />);
      await changeSetting('Stop allocating shards at', '80', '% disk used');

      await act(async () => {
        await managedConfigurationApi.update({ [shardLimitName]: null });
        await queryClient.invalidateQueries();
      });

      await screen.findByText('Saved shard limit: 1500');
      expect(screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' })).toHaveValue(1000);
      expect(screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' })).not.toHaveAccessibleDescription(
        /Source:/,
      );
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(80);

      const update = jest.spyOn(managedConfigurationApi, 'update');
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%' }, null));
      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' })).toHaveValue(1500);
      expect(screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' })).toHaveAccessibleDescription(
        /Source: Configuration file/,
      );
    });

    it('keeps the saved value and baseline when an older read finishes after saving', async () => {
      const { queryClient } = renderPage(storageSection, <SavedShardLimit />);
      await screen.findByLabelText('Shard limit per Data Node');
      let finishRead: (data: typeof sampleClusterConfiguration) => void;
      const pendingRead = new Promise<typeof sampleClusterConfiguration>((resolve) => {
        finishRead = resolve;
      });
      jest.spyOn(managedConfigurationApi, 'get').mockReturnValueOnce(pendingRead);
      let refresh: Promise<void>;
      act(() => {
        refresh = queryClient.refetchQueries();
      });
      await changeSetting('Shard limit per Data Node', '1200');
      await userEvent.click(getApplyButton());
      await screen.findByText('Saved shard limit: 1200');
      await waitFor(() => expect(getApplyButton()).toBeDisabled());

      await act(async () => {
        finishRead(sampleClusterConfiguration);
        await refresh;
      });

      await changeSetting('Shard limit per Data Node', '1300');
      await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);
      expect(screen.getByText('Saved shard limit: 1200')).toBeInTheDocument();
      expect(getApplyButton()).toBeDisabled();
    });

    it('excludes removed parameters from a save while preserving the remaining draft', async () => {
      const { queryClient } = renderPage();
      await changeSetting('Stop allocating shards at', '80', '% disk used');
      await changeSetting('Shard limit per Data Node', '1200');
      const update = jest.spyOn(managedConfigurationApi, 'update');

      await act(async () => {
        queryClient.setQueryData(['datanode', 'managed-configuration', null], {
          ...sampleClusterConfiguration,
          parameters: sampleClusterConfiguration.parameters.filter(({ name }) => name !== lowWatermarkName),
        });
      });

      await waitFor(() => expect(screen.queryByLabelText(/^Stop allocating shards at \(/)).not.toBeInTheDocument());
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1200);
      await userEvent.click(getApplyButton());
      await waitFor(() => expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null));
    });

    it('discards an obsolete draft when its parameter is removed from the catalog', async () => {
      const configuration = {
        ...sampleClusterConfiguration,
        parameters: [
          ...sampleClusterConfiguration.parameters,
          configurationParameter({
            name: 'additional_setting',
            label: 'Additional setting',
            category: 'Storage and Capacity',
            scope: 'cluster',
          }),
        ],
      };
      mockManagedConfigurationApi([configuration]);
      const { queryClient } = renderPage();
      await userEvent.type(await screen.findByLabelText('Additional setting'), 'enabled');

      await act(async () => {
        queryClient.setQueryData(['datanode', 'managed-configuration', null], sampleClusterConfiguration);
      });

      await waitFor(() => expect(screen.queryByLabelText('Additional setting')).not.toBeInTheDocument());
      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();

      await act(async () => {
        queryClient.setQueryData(['datanode', 'managed-configuration', null], configuration);
      });

      expect(await screen.findByLabelText('Additional setting')).toHaveValue('');
      expect(getApplyButton()).toBeDisabled();
    });

    it.each([
      { scenario: 'a clean form', editFirst: false },
      { scenario: 'a canceled draft', editFirst: true },
    ])('adopts newer backend values and their baseline for $scenario', async ({ editFirst }) => {
      const { queryClient } = renderPage();
      expect(await screen.findByLabelText('Shard limit per Data Node')).toHaveValue(1000);
      if (editFirst) await changeSetting('Stop allocating shards at', '80', '% disk used');

      await act(async () => {
        await managedConfigurationApi.update({ [shardLimitName]: '1500' });
        await queryClient.invalidateQueries();
      });
      if (editFirst) {
        await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      }

      await waitFor(() => expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1500));
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(85);
      expect(getApplyButton()).toBeDisabled();
      await changeSetting('Shard limit per Data Node', '1200');
      await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1500);
      expect(getApplyButton()).toBeDisabled();
    });

    it('keeps the draft when a background refresh fails and retries without remounting the editor', async () => {
      const transport = mockManagedConfigurationApi([sampleClusterConfiguration]);
      const { queryClient } = renderPage();
      await changeSetting('Stop allocating shards at', '80', '% disk used');
      transport.mockRejectedValueOnce(new Error('Unavailable'));

      await act(async () => {
        await queryClient.invalidateQueries();
      });

      await screen.findByText(
        'Could not refresh Data Node configuration. Your changes have been kept. Please try again.',
      );
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(80);
      await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

      await waitFor(() => expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument());
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(80);
      expect(getApplyButton()).toBeEnabled();
    });
  });

  describe('Units and validation', () => {
    it('shows percentage units in labels and accepts only numbers in the value field', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
      expect(input).toHaveValue(85);
      await userEvent.clear(input);
      await userEvent.type(input, '88%');
      expect(input).toHaveValue(88);
      await userEvent.click(getApplyButton());
      await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '88%' }, null));
    });

    it.each([
      { unitLabel: 'GB free', amount: '20' },
      { unitLabel: 'Ratio (0–1)', amount: '0.8' },
    ])(
      'keeps $unitLabel when clearing the number and restores the saved unit on cancel',
      async ({ unitLabel, amount }) => {
        renderPage();
        const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
        expect(input).toHaveValue(85);
        await userEvent.click(screen.getByRole('button', { name: 'Stop allocating shards at unit' }));
        await userEvent.click(await screen.findByRole('menuitem', { name: unitLabel }));
        await userEvent.clear(input);
        expect(screen.getByRole('button', { name: 'Stop allocating shards at unit' })).toHaveTextContent(unitLabel);
        expect(screen.getByRole('spinbutton', { name: `Stop allocating shards at (${unitLabel})` })).toHaveValue(null);
        await userEvent.type(input, amount);
        expect(screen.getByRole('spinbutton', { name: `Stop allocating shards at (${unitLabel})` })).toHaveValue(
          Number(amount),
        );

        await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(85);
        expect(getApplyButton()).toBeDisabled();
      },
    );

    it('selects a unit without a suffix on an empty field and saves a bare numeric value', async () => {
      mockManagedConfigurationApi();
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
      await userEvent.click(screen.getByRole('button', { name: 'Stop allocating shards at unit' }));
      await userEvent.click(await screen.findByRole('menuitem', { name: 'Ratio (0–1)' }));
      const input = screen.getByRole('spinbutton', { name: 'Stop allocating shards at (Ratio (0–1))' });
      expect(input).toHaveValue(null);
      expect(input).toHaveAttribute('min', '0');
      expect(input).toHaveAttribute('max', '1');
      await userEvent.type(input, '0.85');
      await userEvent.click(getApplyButton());
      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '0.85' }, null);
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (Ratio (0–1))' })).toHaveValue(0.85);
    });

    it('loads saved ratios without changing them and saves only edited fields', async () => {
      await managedConfigurationApi.update({
        [lowWatermarkName]: '0.85',
        'cluster.routing.allocation.disk.watermark.high': '0.9',
        'cluster.routing.allocation.disk.watermark.flood_stage': '0.95',
      });
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      expect(await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (Ratio (0–1))' })).toHaveValue(
        0.85,
      );
      const input = screen.getByRole('spinbutton', { name: 'Stop allocating shards at (Ratio (0–1))' });
      await userEvent.clear(input);
      expect(screen.getByRole('button', { name: 'Stop allocating shards at unit' })).toHaveTextContent('Ratio (0–1)');
      await userEvent.type(input, '0.85');
      expect(getApplyButton()).toBeDisabled();

      await changeSetting('Shard limit per Data Node', '1200');
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null));
      expect(await savedValueOf(lowWatermarkName)).toBe('0.85');
    });

    it('accepts a fractional numeric value without requiring a leading zero', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
      await userEvent.clear(input);
      await userEvent.type(input, '.5');
      expect(input).toHaveValue(0.5);
      await userEvent.click(getApplyButton());
      await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '0.5%' }, null));
    });

    it('validates the shard limit against the minimum the API provides', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const error = 'Enter a number of at least 1.';
      renderPage();

      await changeSetting('Shard limit per Data Node', '0');
      expect(screen.queryByText(error)).not.toBeInTheDocument();
      await userEvent.tab();
      await screen.findByText(error);
      expect(update).not.toHaveBeenCalled();
      await userEvent.click(getApplyButton());

      await screen.findByText(error);
      const invalidInput = screen.getByRole('spinbutton', { name: 'Shard limit per Data Node' });
      expect(invalidInput).toHaveAttribute('aria-invalid', 'true');
      expect(invalidInput).toHaveAccessibleDescription(new RegExp(escapeRegExp(error)));
      expect(update).not.toHaveBeenCalled();

      await changeSetting('Shard limit per Data Node', '1200');
      await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null));
    });

    it('rejects a quantity without an amount against the pattern the API provides', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const error = 'Enter a value in a supported format.';
      renderPage();

      const input = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
      await userEvent.clear(input);
      await userEvent.click(getApplyButton());

      await screen.findByText(error);
      expect(update).not.toHaveBeenCalled();

      await userEvent.type(input, '80');
      await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(update).toHaveBeenCalledWith({ [lowWatermarkName]: '80%' }, null));
    });

    it.each([
      {
        scenario: 'disk usage watermarks',
        unit: '% disk used',
        low: '99',
        high: '90',
        flood: '95',
        correctedLow: '80',
        expectedPatch: { [lowWatermarkName]: '80%' },
        error: 'Set watermarks in ascending order: low, high, then flood stage.',
      },
      {
        scenario: 'free-space watermarks',
        unit: 'GB free',
        low: '5',
        high: '10',
        flood: '3',
        correctedLow: '20',
        expectedPatch: {
          [lowWatermarkName]: '20gb',
          'cluster.routing.allocation.disk.watermark.high': '10gb',
          'cluster.routing.allocation.disk.watermark.flood_stage': '3gb',
        },
        error: 'Set free-space watermarks in descending order: low, high, then flood stage.',
      },
    ])(
      'validates the order of $scenario and applies corrected values',
      async ({ unit, low, high, flood, correctedLow, expectedPatch, error }) => {
        const update = jest.spyOn(managedConfigurationApi, 'update');
        renderPage();

        await changeSetting('Stop allocating shards at', low, unit);
        await changeSetting('Move shards away at', high, unit);
        await changeSetting('Protect indexes at', flood, unit);
        await userEvent.click(getApplyButton());

        await screen.findByText(error);
        expect(update).not.toHaveBeenCalled();

        await changeSetting('Stop allocating shards at', correctedLow, unit);
        await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
        await userEvent.click(getApplyButton());

        await waitFor(() => expect(update).toHaveBeenCalledWith(expectedPatch, null));
      },
    );

    it('rejects mixed watermark units and applies a consistent set of free-space values', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeSetting('Stop allocating shards at', '20', 'GB free');
      await userEvent.click(getApplyButton());

      const error = 'Use disk usage units for all three watermarks, or use free-space units for all three.';
      await screen.findByText(error);
      expect(update).not.toHaveBeenCalled();

      await changeSetting('Move shards away at', '10', 'GB free');
      await changeSetting('Protect indexes at', '5', 'GB free');
      await waitFor(() => expect(screen.queryByText(error)).not.toBeInTheDocument());
      await userEvent.click(getApplyButton());

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
  });

  describe('Reset values and sources', () => {
    it('keeps a failed reset and supports cancel after a backend rejection', async () => {
      const error = new FetchError('Bad Request', 400, { message: 'This setting is locked.' });
      const update = jest.spyOn(managedConfigurationApi, 'update').mockRejectedValueOnce(error);
      renderPage();

      await userEvent.click(await screen.findByRole('button', { name: 'Reset value for Shard limit per Data Node' }));
      await userEvent.click(getApplyButton());

      await screen.findByText(/Your changes have been kept/);
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Could not save Data Node configuration. This setting is locked. Your changes have been kept. Please try again.',
      );
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(null);
      expect(getApplyButton()).toBeEnabled();
      expect(await savedValueOf(shardLimitName)).toBe('1000');

      await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(1000);
      expect(getApplyButton()).toBeDisabled();
      expect(update).toHaveBeenCalledTimes(1);
    });

    it('resets a stored value without a known fallback by sending null on apply', async () => {
      const transport = mockManagedConfigurationApi([managedConfigurationResponse({ [shardLimitName]: '1200' })]);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      expect(await screen.findByLabelText('Shard limit per Data Node')).toHaveValue(1200);
      const source = screen.getByRole('button', { name: 'About the value for Shard limit per Data Node' });
      await userEvent.hover(source);
      expect(await screen.findByRole('tooltip')).toHaveTextContent(
        'This value is saved in the application and takes precedence over configuration files, environment variables, and Java system properties.',
      );
      await userEvent.unhover(source);
      await userEvent.click(screen.getByRole('button', { name: 'Reset value for Shard limit per Data Node' }));
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveValue(null);
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveAttribute('placeholder', 'Enter a value');
      expect(
        screen.queryByRole('button', { name: 'About the value for Shard limit per Data Node' }),
      ).not.toBeInTheDocument();
      expect(update).not.toHaveBeenCalled();

      await userEvent.click(getApplyButton());

      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(transport).toHaveBeenLastCalledWith(
        'PUT',
        '/system/managed_configuration',
        { node_type: 'datanode', node_id: null, values: { [shardLimitName]: null } },
        expect.any(Object),
        expect.any(Object),
      );
      expect(await savedValueOf(shardLimitName)).toBeNull();
    });

    it('does not store an untouched reported value when another field is applied', async () => {
      const configuration = managedConfigurationResponse({ [shardLimitName]: '1000' });
      mockManagedConfigurationApi([
        {
          ...configuration,
          parameters: configuration.parameters.map((parameter) =>
            parameter.name === lowWatermarkName
              ? { ...parameter, node_value: '85%', node_source: 'config_file' }
              : parameter,
          ),
        },
      ]);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      const watermark = await screen.findByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' });
      expect(watermark).toHaveValue(85);
      expect(watermark).toHaveAccessibleDescription(/Source: Configuration file/);
      await changeSetting('Shard limit per Data Node', '1200');
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(update).toHaveBeenCalledWith({ [shardLimitName]: '1200' }, null);
      const saved = await managedConfigurationApi.get();
      expect(saved.parameters.find(({ name }) => name === lowWatermarkName)?.set_value).toBeNull();
      expect(watermark).toHaveValue(85);
      expect(watermark).toHaveAccessibleDescription(/Source: Configuration file/);
    });

    it('validates watermark order against the fallback while keeping the reset in the save request', async () => {
      mockManagedConfigurationApi([
        {
          ...sampleClusterConfiguration,
          parameters: sampleClusterConfiguration.parameters.map((parameter) =>
            parameter.name === lowWatermarkName
              ? { ...parameter, node_value: '92%', node_source: 'config_file' }
              : parameter,
          ),
        },
      ]);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await userEvent.click(await screen.findByRole('button', { name: 'Reset value for Stop allocating shards at' }));
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(92);
      await userEvent.click(getApplyButton());

      await screen.findByText('Set watermarks in ascending order: low, high, then flood stage.');
      expect(update).not.toHaveBeenCalled();

      await changeSetting('Move shards away at', '94', '% disk used');
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(update).toHaveBeenCalledWith(
        { [lowWatermarkName]: null, 'cluster.routing.allocation.disk.watermark.high': '94%' },
        null,
      );
    });
  });

  describe('Permissions', () => {
    it('shows saved settings without apply actions for read-only users', async () => {
      asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(['clusterconfigentry:read']).build());
      renderPage();

      const input = await screen.findByLabelText(/^Stop allocating shards at \(/);
      expect(input).toBeDisabled();
      expect(input).toHaveValue(85);
      expect(screen.getByRole('button', { name: 'Stop allocating shards at unit' })).toBeDisabled();
      expect(screen.queryByRole('button', { name: /apply changes/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();
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

      expect(await screen.findByRole('button', { name: /^Reset value/ })).toBeDisabled();
      expect(screen.getByLabelText('Shard limit per Data Node')).not.toBeDisabled();
    });
  });

  describe('Loading', () => {
    it('loads an empty scope without sample defaults and saves only an edited parameter', async () => {
      const transport = mockManagedConfigurationApi();
      const configuration = managedConfigurationResponse();
      const omitUnknownValues = (response: typeof configuration) => ({
        ...omitBy(response, (value) => value == null),
        parameters: response.parameters.map((parameter) => omitBy(parameter, (value) => value == null)),
      });
      transport.mockResolvedValueOnce(omitUnknownValues(configuration));
      const get = jest.spyOn(managedConfigurationApi, 'get');
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const notify = jest.spyOn(UserNotification, 'success');
      renderPage();

      expect(await screen.findByLabelText('Shard limit per Data Node')).toHaveValue(null);
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveAttribute('placeholder', 'Enter a value');
      expect(screen.getByLabelText('Shard limit per Data Node')).toHaveAccessibleDescription(
        /cluster\.max_shards_per_node/,
      );
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveValue(null);
      expect(screen.getByRole('spinbutton', { name: 'Stop allocating shards at (% disk used)' })).toHaveAttribute(
        'placeholder',
        'Enter a value',
      );
      expect(screen.queryByText(/Source:/)).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /^Reset value/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /About the value/ })).not.toBeInTheDocument();
      expect(getApplyButton()).toBeDisabled();
      expect(await get.mock.results[0].value).toEqual(configuration);

      await changeSetting('Shard limit per Data Node', '1200');
      const saved = managedConfigurationResponse({ [shardLimitName]: '1200' });
      transport.mockResolvedValueOnce(omitUnknownValues(saved));
      await userEvent.click(getApplyButton());

      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(transport).toHaveBeenCalledWith(
        'PUT',
        '/system/managed_configuration',
        { node_type: 'datanode', node_id: null, values: { [shardLimitName]: '1200' } },
        expect.any(Object),
        expect.any(Object),
      );
      expect(notify).toHaveBeenCalledWith('Configuration saved.');
      expect(await update.mock.results[0].value).toEqual(saved);
    });

    it('retries a failed initial load using the keyboard without falling back to samples', async () => {
      const transport = mockManagedConfigurationApi();
      transport.mockRejectedValueOnce(new Error('Unavailable'));
      renderPage();

      await screen.findByText('Could not load Data Node configuration. Please try again.');
      expect(screen.queryByLabelText('Shard limit per Data Node')).not.toBeInTheDocument();
      screen.getByRole('button', { name: 'Retry' }).focus();
      await userEvent.keyboard('{Enter}');
      await screen.findByLabelText('Shard limit per Data Node');
      expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
    });

    it('keeps an empty catalog distinct from a failed load', async () => {
      const transport = mockManagedConfigurationApi();
      transport.mockResolvedValueOnce({ ...managedConfigurationResponse(), parameters: [] });
      renderPage();

      await screen.findByText('No Data Node configuration parameters are available here.');
      expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
      expect(screen.queryByRole('form', { name: 'Data Node configuration' })).not.toBeInTheDocument();
    });
  });
});
