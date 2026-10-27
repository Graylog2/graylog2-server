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
import changeConfigurationField from 'components/datanode/configuration/testing/changeConfigurationField';
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
    it('keeps the draft through a failed node details refresh', async () => {
      const router = renderPage();
      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
      const details: ReturnType<typeof useDataNode> = asMock(useDataNode).mock.results[0].value;
      asMock(useDataNode).mockReturnValue({ ...details, error: new Error('Polling failed') });

      await act(async () => {
        await router.navigate(`${nodePath}/${nodeSettingsSection}?poll=failed`);
      });

      expect(screen.getByRole('heading', { name: `Data Nodes: ${dataNodes[0].hostname}` })).toBeInTheDocument();
      expect(screen.getByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      expect(getApplyButton()).toBeEnabled();
    });
  });

  describe('Saving', () => {
    it('saves the heap size only for this node', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();

      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
      await save();

      expect(getApplyButton()).toHaveAccessibleName('Apply changes');
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g' }, nodeId);
      expect(await savedValueOf('opensearch_heap', nodeId)).toBe('4g');
      expect(await savedValueOf('opensearch_heap', otherNodeId)).toBe('2g');
    });

    it('saves a node setting and isolates cached values between nodes', async () => {
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage('search');

      expect(await screen.findByLabelText(/^Query cache size \(/)).toHaveValue(10);
      await changeConfigurationField('Query cache size', '15', '% of heap');
      await save();

      expect(update).toHaveBeenCalledWith({ [cacheSizeName]: '15%' }, nodeId);

      await userEvent.click(screen.getByRole('link', { name: 'Other Data Node' }));
      await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(10));

      await userEvent.click(screen.getByRole('link', { name: 'First Data Node' }));
      await waitFor(() => expect(screen.getByLabelText(/^Query cache size \(/)).toHaveValue(15));
    });
  });

  describe('Controls and validation', () => {
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
      await changeConfigurationField('OpenSearch JVM heap', '0', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeConfigurationField('Query cache size', '15', '% of heap');
      await userEvent.click(getApplyButton());
      const heap = await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' });
      await waitFor(() => expect(heap).toHaveFocus());
      expect(heap).toHaveValue(0);
      expect(router.state.location.pathname).toBe(`${nodePath}/${nodeSettingsSection}`);
      expect(heap).toHaveAccessibleDescription(/Enter a value in a supported format\./);
      expect(update).not.toHaveBeenCalled();
      expect(confirm).not.toHaveBeenCalled();
      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
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
      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeConfigurationField('Query cache size', '15', '% of heap');
      await userEvent.click(screen.getByRole('link', { name: 'Node Settings' }));
      expect(await screen.findByRole('spinbutton', { name: 'OpenSearch JVM heap (GB)' })).toHaveValue(4);
      expect(screen.getAllByRole('form', { name: 'Data Node configuration' })).toHaveLength(1);
      expect(screen.getAllByRole('button', { name: /apply changes/i })).toHaveLength(1);
      expect(confirm).not.toHaveBeenCalled();
      await save();
      expect(update).toHaveBeenCalledTimes(1);
      expect(update).toHaveBeenCalledWith({ opensearch_heap: '4g', [cacheSizeName]: '15%' }, nodeId);
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      expect(await screen.findByRole('spinbutton', { name: 'Query cache size (% of heap)' })).toHaveValue(15);
      expect(getApplyButton()).toBeDisabled();
      expect(confirm).not.toHaveBeenCalled();
    });

    it('cancels edits in every category without saving or warning on subsequent navigation', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      renderPage();
      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
      await userEvent.click(screen.getByRole('link', { name: 'Search' }));
      await changeConfigurationField('Query cache size', '15', '% of heap');
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

    it('keeps the current node and its draft when switching nodes is declined', async () => {
      const confirm = jest.spyOn(window, 'confirm').mockReturnValue(false);
      const update = jest.spyOn(managedConfigurationApi, 'update');
      const router = renderPage();
      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
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
      await changeConfigurationField('OpenSearch JVM heap', '4', 'GB');
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
