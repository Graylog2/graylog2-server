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
import { act, renderUnwrapped, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import DefaultProviders from 'DefaultProviders';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';
import { defaultUser } from 'defaultMockValues';
import { dataRouterFuture, routerProviderFuture } from 'reactRouterFutureFlags';

import { dataNodes } from 'fixtures/dataNodes';
import selectEvent from 'helpers/selectEvent';
import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import useDataNode from 'components/datanode/hooks/useDataNode';
import { Link } from 'components/common';
import AppConfig from 'util/AppConfig';
import mockManagedConfigurationApi, {
  managedConfigurationResponse,
  sampleClusterConfiguration,
  sampleNodeConfiguration,
} from 'components/datanode/configuration/testing/mockManagedConfigurationApi';
import changeSetting from 'components/datanode/configuration/testing/changeConfigurationField';
import managedConfigurationApi from 'components/datanode/configuration/managedConfigurationApi';
import RoutePaths from 'routing/Routes';

import DataNodePage from './DataNodePage';

jest.mock('hooks/useCurrentUser');
jest.mock('routing/request');
jest.mock('components/datanode/hooks/useDataNode');
jest.mock('components/cluster-configuration/ClusterConfigurationPageNavigation', () => () => null);
jest.mock('components/datanode/DataNodeList/DataNodeActions', () => () => null);
jest.mock('components/datanode/DataNodeConfiguration/CertificateRenewal', () => ({ CertRenewalButton: () => null }));

const nodeId = dataNodes[0].node_id;
const otherNodeId = dataNodes[1].node_id;
const nodePath = RoutePaths.SYSTEM.CLUSTER.DATANODE_SHOW(nodeId);
const otherNodePath = RoutePaths.SYSTEM.CLUSTER.DATANODE_SHOW(otherNodeId);
const nodeSettingsSection = 'node-settings';
const cacheSizeName = 'indices.queries.cache.size';

const savedValueOf = async (name: string, id: string | null = null) => {
  const configuration = await managedConfigurationApi.get(id);

  return configuration.parameters.find((parameter) => parameter.name === name)?.value;
};

const renderPage = (section = nodeSettingsSection) => {
  const router = createMemoryRouter(
    [
      {
        path: `${RoutePaths.SYSTEM.CLUSTER.DATANODE_SHOW(':dataNodeId')}/*`,
        element: (
          <>
            <DataNodePage />
            <Link to={`${otherNodePath}/search`}>Other Data Node</Link>
            <Link to={`${nodePath}/search`}>First Data Node</Link>
          </>
        ),
      },
      { path: '/notfound', element: <h1>Page not found</h1> },
    ],
    {
      initialEntries: [section ? `${nodePath}/${section}` : nodePath],
      future: dataRouterFuture,
    },
  );
  renderUnwrapped(
    <DefaultQueryClientProvider>
      <DefaultProviders env="test">
        <RouterProvider router={router} future={routerProviderFuture} />
      </DefaultProviders>
    </DefaultQueryClientProvider>,
  );

  return router;
};

const getApplyButton = () => screen.getByRole('button', { name: /apply changes/i });

const save = async () => {
  await userEvent.click(getApplyButton());
  await waitFor(() => expect(getApplyButton()).toHaveTextContent(/^Apply changes$/));
  expect(getApplyButton()).toBeDisabled();
};

describe('DataNodePage configuration', () => {
  beforeEach(() => {
    mockManagedConfigurationApi([
      sampleClusterConfiguration,
      sampleNodeConfiguration(nodeId),
      sampleNodeConfiguration(otherNodeId),
    ]);
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(AppConfig.gl2DevMode).mockReturnValue(false);
    asMock(useDataNode).mockImplementation((id) => ({
      data: {
        ...(dataNodes.find((node) => node.node_id === id) ?? dataNodes[0]),
        version_compatible: true,
        cluster_address: '',
        rest_api_address: '',
        action_queue: '',
        datanode_status: 'AVAILABLE',
      },
      isInitialLoading: false,
      error: null,
      refetch: jest.fn(),
    }));
  });

  afterEach(() => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
  });

  describe('Rendering and navigation', () => {
    it('preserves node details and opens the reusable editor with the saved heap size', async () => {
      const router = renderPage('');
      const heap = await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });

      expect(router.state.location.pathname).toBe(`${nodePath}/${nodeSettingsSection}`);
      expect(screen.getByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` })).toBeInTheDocument();
      expect(heap).toHaveValue(2);
      expect(heap).toHaveAccessibleDescription(/opensearch_heap · Source: Saved value/);
      expect(screen.getByRole('combobox', { name: 'Node roles' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Node Settings' })).toHaveAttribute('aria-current', 'page');
      expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('href', `${nodePath}/search`);
      expect(screen.getByRole('link', { name: 'Indexing and Recovery' })).toHaveAttribute(
        'href',
        `${nodePath}/indexing-and-recovery`,
      );
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(getApplyButton()).toBeDisabled();
      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
    });

    it('shows only the selected section and keeps the section in the URL', async () => {
      const router = renderPage();
      await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });

      await userEvent.click(screen.getByRole('link', { name: 'Search' }));

      expect(await screen.findByLabelText('Maximum query clauses')).toHaveValue(32768);
      expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(10);
      expect(screen.queryByLabelText(/^OpenSearch JVM heap \(/)).not.toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Search' })).toBeInTheDocument();
      expect(router.state.location.pathname).toBe(`${nodePath}/search`);

      await userEvent.click(screen.getByRole('link', { name: 'Indexing and Recovery' }));

      expect(await screen.findByRole('spinbutton', { name: 'Indexing buffer size (% of heap)' })).toHaveValue(10);
      expect(screen.getByRole('spinbutton', { name: 'Recovery bandwidth limit (MB/s)' })).toHaveValue(40);
      expect(router.state.location.pathname).toBe(`${nodePath}/indexing-and-recovery`);
    });
  });

  describe('Loading', () => {
    it('keeps the draft through a failed node details refresh and recovery', async () => {
      const router = renderPage();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      const details: ReturnType<typeof useDataNode> = asMock(useDataNode).mock.results[0].value;
      asMock(useDataNode).mockReturnValue({ ...details, error: new Error('Polling failed') });

      await act(async () => {
        await router.navigate(`${nodePath}/${nodeSettingsSection}?poll=failed`);
      });

      await screen.findByText('Could not refresh Data Node details. Your changes have been kept. Please try again.');
      expect(screen.getByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` })).toBeInTheDocument();
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
      expect(details.refetch).toHaveBeenCalled();
      asMock(useDataNode).mockReturnValue(details);

      await act(async () => {
        await router.navigate(`${nodePath}/${nodeSettingsSection}?poll=recovered`);
      });

      expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      expect(getApplyButton()).toBeEnabled();
    });

    it('loads node values without inventing heap or role defaults', async () => {
      const transport = mockManagedConfigurationApi();
      const configuration = managedConfigurationResponse({}, nodeId);
      transport.mockResolvedValueOnce({
        ...configuration,
        parameters: configuration.parameters.map(
          ({ value: _value, set_value: _storedValue, node_value: _fallbackValue, ...parameter }) => parameter,
        ),
      });
      renderPage();

      expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(null);
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveAttribute(
        'placeholder',
        'Enter a value',
      );
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveAccessibleDescription(
        /opensearch_heap/,
      );
      expect(screen.getByRole('combobox', { name: 'Node roles' })).toHaveAccessibleDescription(/node_roles/);
      expect(screen.getByText('Select a value')).toBeInTheDocument();
      expect(screen.queryByText(/Source:/)).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /About the value/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /^Reset value/ })).not.toBeInTheDocument();
      expect(getApplyButton()).toBeDisabled();

      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await save();

      expect(transport).toHaveBeenLastCalledWith(
        'PUT',
        '/system/managed_configuration',
        { node_type: 'datanode', node_id: nodeId, values: { opensearch_heap: '4g' } },
        expect.any(Object),
        expect.any(Object),
      );
    });
  });

  describe('Saving and restart behavior', () => {
    it('saves the heap size only for this node', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Node');
      expect(screen.getByRole('alert', { name: 'Restart required after saving' })).toHaveTextContent(
        'Changes to the following settings require restarting this Data Node to take effect:',
      );
      await userEvent.click(screen.getByRole('link', { name: 'OpenSearch JVM heap' }));
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveFocus();
      await save();

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g' }, nodeId);
      expect(await savedValueOf('opensearch_heap', nodeId)).toBe('4g');
      expect(await savedValueOf('opensearch_heap', otherNodeId)).toBe('2g');
    });

    it('saves without restarting and keeps the restart action available for the saved changes', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

      const warning = await screen.findByRole('alert', { name: 'Saved changes require a restart' });
      expect(warning).toHaveTextContent(
        'The following settings have been saved. Restart this Data Node for them to take effect:',
      );
      expect(within(warning).getByRole('link', { name: 'OpenSearch JVM heap' })).toHaveAttribute(
        'href',
        `${nodePath}/${nodeSettingsSection}#opensearch_heap`,
      );
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Restart Data Node' })).toBeEnabled();
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g' }, nodeId);

      await userEvent.click(screen.getByRole('button', { name: 'Restart Data Node' }));

      await waitFor(() => expect(getApplyButton()).toBeDisabled());
      expect(update).toHaveBeenLastCalledWith({}, nodeId);
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
    });

    it('saves a node setting and isolates cached values between nodes', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage('search');

      expect(await screen.findByLabelText(/^Query cache size \(/)).toHaveValue(10);
      await changeSetting('Query cache size', '15', '% of heap');
      await save();

      expect(update).toHaveBeenCalledWith({ [cacheSizeName]: '15%' }, nodeId);

      await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
      await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10));

      await userEvent.click(screen.getByRole('link', { name: 'First Data Node' }));
      await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15));
    });

    it('saves recovery changes only for this node and preserves settings in other sections', async () => {
      renderPage('indexing-and-recovery');

      await changeSetting('Recovery bandwidth limit', '60', 'MB/s');
      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
      await changeSetting('Indexing buffer size', '15', '% of heap');
      expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Node');
      await changeSetting('Indexing buffer size', '10', '% of heap');
      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      await save();

      expect(await savedValueOf('indices.recovery.max_bytes_per_sec', nodeId)).toBe('60mb');
      expect(await savedValueOf('opensearch_heap', nodeId)).toBe('2g');
      expect(await savedValueOf('indices.recovery.max_bytes_per_sec', otherNodeId)).toBe('40mb');
    });
  });

  describe('Controls and validation', () => {
    it('validates save-only changes against the API constraints before allowing apply and restart', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeSetting('OpenSearch JVM heap', '0', 'GB');
      await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

      await screen.findByText('Enter a value in a supported format.');
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(0);
      expect(update).not.toHaveBeenCalled();

      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await save();
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g' }, nodeId);
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('saves node roles as a comma-separated string for this node', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await screen.findByRole('combobox', { name: 'Node roles' });

      await selectEvent.chooseOption('Node roles', 'Search');
      screen.getByRole('link', { name: 'Node roles' }).focus();
      await userEvent.keyboard('{Enter}');
      expect(screen.getByRole('combobox', { name: 'Node roles' })).toHaveFocus();
      await save();

      expect(update).toHaveBeenCalledWith(
        { node_roles: 'cluster_manager,data,ingest,remote_cluster_client,search' },
        nodeId,
      );
    });

    it('opens the category containing the first invalid field and focuses it on save', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const router = renderPage();
      await changeSetting('OpenSearch JVM heap', '0', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeSetting('Query cache size', '15', '% of heap');
      await userEvent.click(getApplyButton());
      const heap = await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });
      await waitFor(() => expect(heap).toHaveFocus());
      expect(heap).toHaveValue(0);
      expect(router.state.location.pathname).toBe(`${nodePath}/${nodeSettingsSection}`);
      expect(heap).toHaveAccessibleDescription(/Enter a value in a supported format\./);
      expect(update).not.toHaveBeenCalled();
      expect(confirm).not.toHaveBeenCalled();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await save();
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g', [cacheSizeName]: '15%' }, nodeId);
    });
  });

  describe('Permissions', () => {
    it('keeps details visible without exposing configuration to users lacking read permission', async () => {
      asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());
      renderPage('');

      await screen.findByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` });
      expect(screen.queryByRole('navigation', { name: 'Data Node configuration sections' })).not.toBeInTheDocument();
    });
  });

  describe('Reset values', () => {
    it.each([
      { scenario: 'unknown', fallback: null, amount: null },
      { scenario: 'different', fallback: '1g', amount: 1 },
      { scenario: 'identical', fallback: '4g', amount: 4 },
    ])(
      'stages resets with $scenario fallbacks, supports cancel and sends null on apply',
      async ({ fallback, amount }) => {
        const configuration = managedConfigurationResponse({ opensearch_heap: '4g' }, nodeId);
        mockManagedConfigurationApi([
          {
            ...configuration,
            parameters: configuration.parameters.map((parameter) =>
              parameter.name === 'opensearch_heap'
                ? { ...parameter, node_value: fallback, node_source: 'config_file' }
                : parameter,
            ),
          },
        ]);
        const update = jest.spyOn(managedConfigurationApi, 'update');
        renderPage();

        const resetButton = await screen.findByRole('button', { name: 'Reset value for OpenSearch JVM heap' });
        const heap = screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });
        await userEvent.click(resetButton);

        expect(heap).toHaveValue(amount);
        expect(heap).toHaveFocus();
        expect(heap).toHaveAccessibleDescription(
          fallback === null ? /^Description of OpenSearch JVM heap\. opensearch_heap$/ : /Source: Configuration file/,
        );
        expect(resetButton).toBeDisabled();
        expect(getApplyButton()).toBeEnabled();
        expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Node');
        expect(update).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(heap).toHaveValue(4);
        expect(heap).toHaveAccessibleDescription(/Source: Saved value/);
        expect(resetButton).toBeEnabled();
        expect(getApplyButton()).toBeDisabled();

        await userEvent.click(resetButton);
        if (fallback !== null) {
          await userEvent.click(screen.getByRole('button', { name: 'OpenSearch JVM heap unit' }));
          await userEvent.click(screen.getByRole('menuitem', { name: 'GB' }));
        }
        await save();

        expect(update).toHaveBeenCalledWith({ opensearch_heap: null }, nodeId);
        expect(heap).toHaveValue(amount);
        expect(screen.queryByRole('button', { name: 'Reset value for OpenSearch JVM heap' })).not.toBeInTheDocument();
      },
    );
  });

  describe('Drafts and navigation', () => {
    it('keeps edits across categories and saves all of them in one request', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await userEvent.click(await screen.findByRole('button', { name: 'Reset value for OpenSearch JVM heap' }));
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeSetting('Query cache size', '15', '% of heap');
      const heapLink = screen.getByRole('link', { name: 'OpenSearch JVM heap' });
      expect(heapLink).toHaveAttribute('href', `${nodePath}/${nodeSettingsSection}#opensearch_heap`);
      await userEvent.click(heapLink);
      expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveFocus();
      expect(screen.getAllByRole('form', { name: 'Data Node configuration' })).toHaveLength(1);
      expect(screen.getAllByRole('button', { name: /apply changes/i })).toHaveLength(1);
      expect(confirm).not.toHaveBeenCalled();
      const restartWarning = screen.getByRole('alert', { name: 'Restart required after saving' });
      expect(
        within(restartWarning)
          .getAllByRole('listitem')
          .map(({ textContent }) => textContent),
      ).toEqual(['OpenSearch JVM heap', 'Query cache size']);
      expect(restartWarning).not.toHaveTextContent('Node roles');
      await changeSetting('OpenSearch JVM heap', '2', 'GB');
      expect(restartWarning).not.toHaveTextContent('OpenSearch JVM heap');
      expect(restartWarning).toHaveTextContent('Query cache size');
      expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Node');
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Query cache size' }));
      expect(await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveFocus();
      await save();
      expect(update).toHaveBeenCalledTimes(1);
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g', [cacheSizeName]: '15%' }, nodeId);
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      expect(await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(15);
      expect(getApplyButton()).toBeDisabled();
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(confirm).not.toHaveBeenCalled();
    });

    it('cancels edits in every category without saving or warning on subsequent navigation', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeSetting('Query cache size', '15', '% of heap');
      await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(10);
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
      await userEvent.click(screen.getByRole('link', { name: 'Node Settings' }));
      expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(2);
      expect(getApplyButton()).toBeDisabled();
      expect(update).not.toHaveBeenCalled();
      await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
      await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' });
      expect(confirm).not.toHaveBeenCalled();
    });

    it('keeps drafts in every category after a failed save and saves them together on retry', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update').mockRejectedValueOnce(new Error('Save failed'));
      renderPage();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeSetting('Query cache size', '15', '% of heap');
      await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
      await screen.findByText(/Your changes have been kept/);
      expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(15);
      await userEvent.click(screen.getByRole('link', { name: 'Node Settings' }));
      expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      await save();
      expect(update).toHaveBeenCalledTimes(2);
      expect(update).toHaveBeenLastCalledWith({ opensearch_heap: '4g', [cacheSizeName]: '15%' }, nodeId);
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('keeps the current node and its draft when switching nodes is declined', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const router = renderPage();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
      expect(confirm).toHaveBeenCalledTimes(1);
      expect(router.state.location.pathname).toBe(`${nodePath}/${nodeSettingsSection}`);
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      expect(update).not.toHaveBeenCalled();
    });

    it('discards the current draft when switching nodes is confirmed', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(true);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const router = renderPage();
      await changeSetting('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
      expect(confirm).toHaveBeenCalledTimes(1);
      expect(await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(10);
      expect(router.state.location.pathname).toBe(`${otherNodePath}/search`);
      await userEvent.click(screen.getByRole('link', { name: 'First Data Node' }));
      await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' });
      await userEvent.click(screen.getByRole('link', { name: 'Node Settings' }));
      expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(2);
      expect(update).not.toHaveBeenCalled();
    });
  });
});
