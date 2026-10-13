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
import mockConfigurationOverridesApi, {
  configurationOverridesResponse,
  sampleGlobalConfiguration,
  sampleNodeConfiguration,
} from 'components/datanode/configuration/__tests__/mockConfigurationOverridesApi';
import changeSetting from 'components/datanode/configuration/__tests__/changeConfigurationField';
import configurationOverridesApi from 'components/datanode/configuration/configurationOverridesApi';
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

const renderPage = (section = 'node') => {
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

const save = async (action = 'Save changes') => {
  await userEvent.click(screen.getByRole('button', { name: action }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
};

describe('DataNodePage configuration', () => {
  beforeEach(() => {
    mockConfigurationOverridesApi([
      sampleGlobalConfiguration,
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
    const heap = await screen.findByRole('combobox', { name: 'OpenSearch JVM heap' });
    expect(router.state.location.pathname).toBe(`${nodePath}/node`);
    expect(screen.getByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` })).toBeInTheDocument();
    expect(heap).toHaveValue('custom');
    expect(screen.getByText(/Automatic sizing is not available yet/)).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Node roles' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Node Settings' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Search' })).toHaveAttribute('href', `${nodePath}/search`);
    expect(screen.queryByRole('link', { name: 'Storage and Capacity' })).not.toBeInTheDocument();
    expect(screen.getByLabelText(/^Custom heap size \(/)).toHaveValue(2);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
  });

  it('saves custom heap only for this node and supports removing the override', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();
    await changeSetting('Custom heap size', '4g');
    await save();
    expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g' }, nodeId);
    expect((await configurationOverridesApi.get(nodeId)).overrides.opensearch_heap.value).toBe('4g');
    expect((await configurationOverridesApi.get(otherNodeId)).overrides.opensearch_heap.value).toBe('2g');
    expect((await configurationOverridesApi.get()).overrides.opensearch_heap).toBeUndefined();
    await userEvent.click(screen.getByRole('button', { name: 'Remove override for OpenSearch JVM heap' }));
    expect(screen.queryByLabelText(/^Custom heap size \(/)).not.toBeInTheDocument();
    await save();
    expect((await configurationOverridesApi.get(nodeId)).overrides.opensearch_heap).toBeUndefined();
  });

  it('validates heap sizes without saving invalid changes', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();
    await userEvent.selectOptions(await screen.findByLabelText('OpenSearch JVM heap'), 'custom');
    await changeSetting('Custom heap size', '0g');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a whole number greater than zero for the heap size.');
    expect(screen.getByLabelText(/^Custom heap size \(/)).toHaveValue(0);
    expect(update).not.toHaveBeenCalled();
  });

  it('saves node roles as a comma-separated string for this node', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage();
    await screen.findByRole('combobox', { name: 'Node roles' });
    await selectEvent.chooseOption('Node roles', 'Search');
    await save();
    expect(update).toHaveBeenCalledWith(
      { node_roles: 'cluster_manager,data,ingest,remote_cluster_client,search' },
      nodeId,
    );
  });

  it('inherits global values, saves a node override, and isolates cached settings between nodes', async () => {
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');
    expect(await screen.findByLabelText(/^Query cache size \(/)).toHaveValue(10);
    expect(screen.getByText('Using global value:')).toBeInTheDocument();
    await changeSetting('Query cache size', '15%');
    await save();
    expect(update).toHaveBeenCalledWith({ 'indices.queries.cache.size': '15%' }, nodeId);
    expect((await configurationOverridesApi.get()).overrides['indices.queries.cache.size'].value).toBe('10%');
    await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
    await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10));
    await userEvent.click(screen.getByRole('link', { name: 'First Data Node' }));
    await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15));
  });

  it('removes a node setting to inherit the latest global value', async () => {
    await configurationOverridesApi.update({ 'indices.queries.cache.size': '15%' }, nodeId);
    await configurationOverridesApi.update({ 'indices.queries.cache.size': '12%' });
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');
    expect(await screen.findByLabelText(/^Query cache size \(/)).toHaveValue(15);
    await userEvent.click(screen.getByRole('button', { name: 'Use global value' }));
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(12);
    await save();
    expect(update).toHaveBeenCalledWith({ 'indices.queries.cache.size': null }, nodeId);
    expect((await configurationOverridesApi.get(nodeId)).overrides['indices.queries.cache.size']).toBeUndefined();
  });

  it('restores both amount and unit when switching from a fixed node memory size to the global percentage', async () => {
    await configurationOverridesApi.update({ 'indices.queries.cache.size': '256mb' }, nodeId);
    renderPage('search');
    expect(await screen.findByRole('spinbutton', { name: 'Query cache size (MB)' })).toHaveValue(256);
    await userEvent.click(screen.getByRole('button', { name: 'Use global value' }));
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(10);
    expect(screen.getByRole('button', { name: 'Query cache size unit' })).toHaveTextContent('% of heap');
    await save();
    expect((await configurationOverridesApi.get(nodeId)).overrides['indices.queries.cache.size']).toBeUndefined();
  });

  it('saves recovery changes only for this node and preserves settings in other sections', async () => {
    renderPage('indexing');
    await changeSetting('Recovery bandwidth limit', '60mb');
    await save();
    const node = await configurationOverridesApi.get(nodeId);
    expect(node.overrides['indices.recovery.max_bytes_per_sec'].value).toBe('60mb');
    expect(node.overrides.opensearch_heap.value).toBe('2g');
    expect((await configurationOverridesApi.get()).overrides['indices.recovery.max_bytes_per_sec'].value).toBe('40mb');
  });

  it('keeps drafts when saving fails and supports cancel', async () => {
    jest.spyOn(configurationOverridesApi, 'update').mockRejectedValueOnce(new Error('Save failed'));
    renderPage('search');
    await changeSetting('Query cache size', '15%');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await screen.findByRole('alert');
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15);
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10);
  });

  it('requires deletion permission to remove a saved node setting', async () => {
    await configurationOverridesApi.update({ 'indices.queries.cache.size': '15%' }, nodeId);
    asMock(useCurrentUser).mockReturnValue(
      defaultUser
        .toBuilder()
        .permissions(['clusterconfigentry:read', 'clusterconfigentry:create', 'clusterconfigentry:edit'])
        .build(),
    );
    renderPage('search');
    await screen.findByLabelText(/^Query cache size \(/);
    expect(screen.getByRole('button', { name: 'Use global value' })).toBeDisabled();
    expect(screen.getByLabelText(/^Query cache size \(/)).not.toBeDisabled();
  });

  it('allows discarding an unsaved node override without deletion permission', async () => {
    asMock(useCurrentUser).mockReturnValue(
      defaultUser
        .toBuilder()
        .permissions(['clusterconfigentry:read', 'clusterconfigentry:create', 'clusterconfigentry:edit'])
        .build(),
    );
    const update = jest.spyOn(configurationOverridesApi, 'update');
    renderPage('search');
    await changeSetting('Query cache size', '15%');
    await userEvent.click(screen.getByRole('button', { name: 'Use global value' }));
    expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10);
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    expect(update).not.toHaveBeenCalled();
  });

  it('provides read-only fields without configuration actions', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(['clusterconfigentry:read']).build());
    renderPage();
    expect(await screen.findByLabelText('OpenSearch JVM heap')).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument();
  });

  it('keeps details visible without exposing configuration to users lacking read permission', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions([]).build());
    renderPage('');
    await screen.findByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` });
    expect(screen.queryByRole('navigation', { name: 'Data Node configuration sections' })).not.toBeInTheDocument();
  });
  it('loads real node overrides without inventing heap or role defaults', async () => {
    const transport = mockConfigurationOverridesApi();
    renderPage('node');
    expect(await screen.findByRole('combobox', { name: 'OpenSearch JVM heap' })).toHaveValue('');
    expect(screen.getByRole('option', { name: /Automatic/ })).toBeDisabled();
    expect(screen.queryByLabelText(/^Custom heap size \(/)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled();
    await userEvent.selectOptions(screen.getByLabelText('OpenSearch JVM heap'), 'custom');
    await changeSetting('Custom heap size', '4g');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/configuration_overrides',
      { node_type: 'datanode', node_id: nodeId, overrides: { opensearch_heap: '4g' } },
      {},
      { Accept: ['application/json'] },
    );
  });

  it('loads both real scopes and removes only the node override to inherit the saved global value', async () => {
    const transport = mockConfigurationOverridesApi([
      configurationOverridesResponse({ 'indices.queries.cache.size': '12%' }),
      configurationOverridesResponse({ 'indices.queries.cache.size': '15%' }, nodeId),
    ]);
    renderPage('search');
    expect(await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(15);
    expect(transport).toHaveBeenCalledWith(
      'GET',
      '/system/configuration_overrides',
      null,
      { node_type: 'datanode', node_id: nodeId },
      { Accept: ['application/json'] },
      undefined,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Use global value' }));
    expect(screen.getByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(12);
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/configuration_overrides',
      { node_type: 'datanode', node_id: nodeId, overrides: { 'indices.queries.cache.size': null } },
      {},
      { Accept: ['application/json'] },
    );
    expect(screen.getByText('Using global value:')).toBeInTheDocument();
  });

  it('allows removing a saved node setting when no global override exists', async () => {
    const transport = mockConfigurationOverridesApi([
      configurationOverridesResponse({ opensearch_heap: '4g' }, nodeId),
    ]);
    renderPage('node');
    await screen.findByRole('button', { name: /^Remove override/ });
    await userEvent.click(screen.getByRole('button', { name: /^Remove override/ }));
    expect(screen.getByRole('combobox', { name: 'OpenSearch JVM heap' })).toHaveValue('');
    expect(screen.queryByLabelText(/^Custom heap size \(/)).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled());
    expect(transport).toHaveBeenLastCalledWith(
      'PUT',
      '/system/configuration_overrides',
      { node_type: 'datanode', node_id: nodeId, overrides: { opensearch_heap: null } },
      {},
      { Accept: ['application/json'] },
    );
  });
});
