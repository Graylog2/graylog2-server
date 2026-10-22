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
import { renderUnwrapped, screen, waitFor, within } from 'wrappedTestingLibrary';
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
} from 'components/datanode/configuration/__tests__/mockManagedConfigurationApi';
import changeSetting from 'components/datanode/configuration/__tests__/changeConfigurationField';
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

  it('preserves node details and opens the reusable editor with the saved heap size', async () => {
    const router = renderPage('');
    const heap = await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });

    expect(router.state.location.pathname).toBe(`${nodePath}/${nodeSettingsSection}`);
    expect(screen.getByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` })).toBeInTheDocument();
    expect(heap).toHaveValue(2);
    expect(screen.getByRole('combobox', { name: 'Node roles' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Node Settings' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('href', `${nodePath}/search`);
    expect(screen.getByRole('link', { name: 'Indexing and Recovery' })).toHaveAttribute(
      'href',
      `${nodePath}/indexing-and-recovery`,
    );
    // Parameters shared by all Data Nodes live on the cluster configuration page
    expect(screen.queryByRole('link', { name: 'Storage and Capacity' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^Stop allocating shards at \(/)).not.toBeInTheDocument();
    expect(screen.queryByText('Restart required')).not.toBeInTheDocument();
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

  it('saves the heap size only for this node and supports removing the stored value', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('OpenSearch JVM heap', '4g');
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

    await userEvent.click(screen.getByRole('button', { name: 'Remove stored value for OpenSearch JVM heap' }));
    expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(null);
    expect(screen.getByRole('alert', { name: 'Restart required after saving' })).toHaveTextContent(
      'OpenSearch JVM heap',
    );
    await userEvent.click(screen.getByRole('link', { name: 'OpenSearch JVM heap' }));
    expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveFocus();
    await save();

    expect(update).toHaveBeenLastCalledWith({ opensearch_heap: null }, nodeId);
    expect(await savedValueOf('opensearch_heap', nodeId)).toBeNull();
  });

  it('validates the heap size against the pattern the API provides', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('OpenSearch JVM heap', '0g');
    await userEvent.click(getApplyButton());

    await screen.findByText('Enter a value in a supported format.');
    expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(0);
    expect(update).not.toHaveBeenCalled();
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

  it('saves a node setting and isolates cached values between nodes', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage('search');

    expect(await screen.findByLabelText(/^Query cache size \(/)).toHaveValue(10);
    await changeSetting('Query cache size', '15%');
    await save();

    expect(update).toHaveBeenCalledWith({ [cacheSizeName]: '15%' }, nodeId);

    await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
    await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10));

    await userEvent.click(screen.getByRole('link', { name: 'First Data Node' }));
    await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15));
  });

  it('saves recovery changes only for this node and preserves settings in other sections', async () => {
    renderPage('indexing-and-recovery');

    await changeSetting('Recovery bandwidth limit', '60mb');
    expect(getApplyButton()).toHaveAccessibleName('Apply changes');
    await changeSetting('Indexing buffer size', '15%');
    expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Node');
    await changeSetting('Indexing buffer size', '10%');
    expect(getApplyButton()).toHaveAccessibleName('Apply changes');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await save();

    expect(await savedValueOf('indices.recovery.max_bytes_per_sec', nodeId)).toBe('60mb');
    expect(await savedValueOf('opensearch_heap', nodeId)).toBe('2g');
    expect(await savedValueOf('indices.recovery.max_bytes_per_sec', otherNodeId)).toBe('40mb');
  });

  it('requires deletion permission to remove a stored node setting', async () => {
    asMock(useCurrentUser).mockReturnValue(
      defaultUser
        .toBuilder()
        .permissions(['clusterconfigentry:read', 'clusterconfigentry:create', 'clusterconfigentry:edit'])
        .build(),
    );
    renderPage('search');

    await screen.findByLabelText(/^Query cache size \(/);
    expect(screen.getByRole('button', { name: 'Remove stored value for Query cache size' })).toBeDisabled();
    expect(screen.getByLabelText(/^Query cache size \(/)).not.toBeDisabled();
  });

  it('provides read-only fields without configuration actions', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(['clusterconfigentry:read']).build());
    renderPage();

    expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: /apply changes/i })).not.toBeInTheDocument();
  });

  it('keeps details visible without exposing configuration to users lacking read permission', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());
    renderPage('');

    await screen.findByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` });
    expect(screen.queryByRole('navigation', { name: 'Data Node configuration sections' })).not.toBeInTheDocument();
  });

  it('loads node values without inventing heap or role defaults', async () => {
    const transport = mockManagedConfigurationApi();
    const configuration = managedConfigurationResponse({}, nodeId);
    transport.mockResolvedValueOnce({
      ...configuration,
      parameters: configuration.parameters.map(
        ({ set_value: _storedValue, node_value: _fallbackValue, ...parameter }) => parameter,
      ),
    });
    renderPage();

    expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(null);
    expect(screen.queryByRole('button', { name: /^Remove stored value/ })).not.toBeInTheDocument();
    expect(screen.queryByText(/Falls back to/)).not.toBeInTheDocument();
    expect(getApplyButton()).toBeDisabled();
    expect(transport).toHaveBeenCalledWith(
      'GET',
      '/system/managed_configuration',
      null,
      { node_type: 'datanode', node_id: nodeId },
      { Accept: ['application/json'] },
      undefined,
    );

    await changeSetting('OpenSearch JVM heap', '4g');
    await save();

    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/managed_configuration',
      { node_type: 'datanode', node_id: nodeId, values: { opensearch_heap: '4g' } },
      {},
      { Accept: ['application/json'] },
    );
  });

  it('removes a stored node setting by sending null', async () => {
    const configuration = managedConfigurationResponse({ opensearch_heap: '4g' }, nodeId);
    const transport = mockManagedConfigurationApi([configuration]);
    transport.mockResolvedValueOnce({
      ...configuration,
      parameters: configuration.parameters.map(({ node_value: _fallbackValue, ...parameter }) => parameter),
    });
    renderPage();

    await screen.findByRole('button', { name: /^Remove stored value/ });
    expect(screen.queryByText(/Falls back to/)).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Remove stored value for OpenSearch JVM heap' }));
    expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(null);

    await save();

    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/managed_configuration',
      { node_type: 'datanode', node_id: nodeId, values: { opensearch_heap: null } },
      {},
      { Accept: ['application/json'] },
    );
  });
  it('keeps edits across categories and saves all of them in one request', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();
    await changeSetting('OpenSearch JVM heap', '4g');
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));
    await changeSetting('Query cache size', '15%');
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
    await changeSetting('OpenSearch JVM heap', '2g');
    expect(restartWarning).not.toHaveTextContent('OpenSearch JVM heap');
    expect(restartWarning).toHaveTextContent('Query cache size');
    expect(getApplyButton()).toHaveAccessibleName('Apply changes and restart Data Node');
    await changeSetting('OpenSearch JVM heap', '4g');
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
    await changeSetting('OpenSearch JVM heap', '4g');
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));
    await changeSetting('Query cache size', '15%');
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

  it('opens the category containing the first invalid field and focuses it on save', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const update = jest.spyOn(managedConfigurationApi, 'update');
    const router = renderPage();
    await changeSetting('OpenSearch JVM heap', '0g');
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));
    await changeSetting('Query cache size', '15%');
    await userEvent.click(getApplyButton());
    const heap = await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });
    await waitFor(() => expect(heap).toHaveFocus());
    expect(heap).toHaveValue(0);
    expect(router.state.location.pathname).toBe(`${nodePath}/${nodeSettingsSection}`);
    expect(heap).toHaveAccessibleDescription(/Enter a value in a supported format\./);
    expect(update).not.toHaveBeenCalled();
    expect(confirm).not.toHaveBeenCalled();
    await changeSetting('OpenSearch JVM heap', '4g');
    await save();
    expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g', [cacheSizeName]: '15%' }, nodeId);
  });

  it('keeps drafts in every category after a failed save and saves them together on retry', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update').mockRejectedValueOnce(new Error('Save failed'));
    renderPage();
    await changeSetting('OpenSearch JVM heap', '4g');
    await userEvent.click(screen.getByRole('link', { name: 'Search' }));
    await changeSetting('Query cache size', '15%');
    await userEvent.click(getApplyButton());
    await screen.findByText(/Your changes have been kept/);
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(15);
    await userEvent.click(screen.getByRole('link', { name: 'Node Settings' }));
    expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
    await save();
    expect(update).toHaveBeenCalledTimes(2);
    expect(update).toHaveBeenLastCalledWith({ opensearch_heap: '4g', [cacheSizeName]: '15%' }, nodeId);
  });

  it('keeps the current node and its draft when switching nodes is declined', async () => {
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
    const update = jest.spyOn(managedConfigurationApi, 'update');
    const router = renderPage();
    await changeSetting('OpenSearch JVM heap', '4g');
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
    await changeSetting('OpenSearch JVM heap', '4g');
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
