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
import { renderUnwrapped, screen, waitFor } from 'wrappedTestingLibrary';
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

const save = async () => {
  await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
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
    expect(screen.getAllByText('Restart required')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
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
    await save();

    expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g' }, nodeId);
    expect(await savedValueOf('opensearch_heap', nodeId)).toBe('4g');
    expect(await savedValueOf('opensearch_heap', otherNodeId)).toBe('2g');

    await userEvent.click(screen.getByRole('button', { name: 'Remove stored value for OpenSearch JVM heap' }));
    expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(null);
    await save();

    expect(update).toHaveBeenLastCalledWith({ opensearch_heap: null }, nodeId);
    expect(await savedValueOf('opensearch_heap', nodeId)).toBeNull();
  });

  it('validates the heap size against the pattern the API provides', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();

    await changeSetting('OpenSearch JVM heap', '0g');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a value in a supported format.');
    expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(0);
    expect(update).not.toHaveBeenCalled();
  });

  it('saves node roles as a comma-separated string for this node', async () => {
    const update = jest.spyOn(managedConfigurationApi, 'update');
    renderPage();
    await screen.findByRole('combobox', { name: 'Node roles' });

    await selectEvent.chooseOption('Node roles', 'Search');
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
    await save();

    expect(await savedValueOf('indices.recovery.max_bytes_per_sec', nodeId)).toBe('60mb');
    expect(await savedValueOf('opensearch_heap', nodeId)).toBe('2g');
    expect(await savedValueOf('indices.recovery.max_bytes_per_sec', otherNodeId)).toBe('40mb');
  });

  it('keeps drafts when saving fails and supports cancel', async () => {
    jest.spyOn(managedConfigurationApi, 'update').mockRejectedValueOnce(new Error('Save failed'));
    renderPage('search');

    await changeSetting('Query cache size', '15%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await screen.findByRole('alert');
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15);

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10);
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
    expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
  });

  it('keeps details visible without exposing configuration to users lacking read permission', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());
    renderPage('');

    await screen.findByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` });
    expect(screen.queryByRole('navigation', { name: 'Data Node configuration sections' })).not.toBeInTheDocument();
  });

  it('loads node values without inventing heap or role defaults', async () => {
    const transport = mockManagedConfigurationApi();
    renderPage();

    expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(null);
    expect(screen.queryByRole('button', { name: /^Remove stored value/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
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
    const transport = mockManagedConfigurationApi([managedConfigurationResponse({ opensearch_heap: '4g' }, nodeId)]);
    renderPage();

    await screen.findByRole('button', { name: /^Remove stored value/ });
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
});
