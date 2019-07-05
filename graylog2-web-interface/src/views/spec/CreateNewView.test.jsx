/**
 * @jest-environment <rootDir>/test/integration-environment.js
 */
// @flow strict
import * as React from 'react';
import { wait, render, fireEvent } from '@testing-library/react';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { StoreMock as MockStore } from 'helpers/mocking';
import history from 'util/History';
import Routes from 'routing/Routes';
import AppRouter from 'routing/AppRouter';

import viewsBindings from 'views/bindings';

jest.mock('legacy/analyzers/fieldcharts', () => ({
  FieldChart: {
    reload: jest.fn(),
  },
}));
jest.mock('components/search/LegacyHistogram', () => 'legacy-histogram');
jest.mock('stores/users/CurrentUserStore', () => MockStore(
  'listen',
  'get',
  ['getInitialState', () => ({
    currentUser: {
      full_name: 'Betty Holberton',
      username: 'betty',
      permissions: [],
    },
  })],
));

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: (function () {
    const express = require('express');
    const cors = require('cors');
    const prefix = '/api/';
    const api = express();
    api.use(cors());
    api.use(express.json());

    const searches = {};

    api.get(prefix, (req, res) => res.json({
      cluster_id: 'deadbeef',
      node_id: 'deadbeef',
      version: '3.0.0',
      tagline: 'Manage your logs in the dark and have lasers going and make it look like you\'re from space!',
    }));
    api.post(`${prefix}cluster/metrics/multiple`, (req, res) => res.json({}));
    api.get(`${prefix}dashboards`, (req, res) => res.json([]));
    api.get(`${prefix}search/decorators/available`, (req, res) => res.json({}));
    api.get(`${prefix}search/decorators`, (req, res) => res.json([]));
    api.get(`${prefix}streams`, (req, res) => res.json({ total: 0, streams: [] }));
    api.get(`${prefix}system`, (req, res) => res.json({}));
    api.get(`${prefix}system/gettingstarted`, (req, res) => res.json({ show: false }));
    api.get(`${prefix}system/jvm`, (req, res) => res.json({}));
    api.get(`${prefix}system/locales`, (req, res) => res.json({ locales: {} }));
    api.get(`${prefix}system/sessions`, (req, res) => res.json({ session_id: null, username: null, is_valid: false }));
    api.get(`${prefix}system/notifications`, (req, res) => res.json([]));
    api.get(`${prefix}system/cluster/nodes`, (req, res) => res.json({ nodes: [], total: 0 }));
    api.get(`${prefix}system/cluster_config/org.graylog2.indexer.searches.SearchesClusterConfig`, (req, res) => res.json({
      relative_timerange_options: {},
      query_time_range_limit: 'P5M',
    }));
    api.get(`${prefix}views`, (req, res) => res.json({
      total: 0,
      page: 1,
      per_page: 1,
      count: 0,
      views: [],
    }));
    api.get(`${prefix}views/fields`, (req, res) => res.json([]));
    api.get(`${prefix}views/functions`, (req, res) => res.json([]));
    api.post(`${prefix}views/search`, (req, res) => {
      const search = req.body;
      searches[search.id] = search;
      return res.json(search);
    });
    api.post(`${prefix}views/search/metadata`, (req, res) => res.json({
      query_metadata: {},
      declared_parameters: {},
    }));
    api.post(/views\/search\/(\w+)\/execute$/, (req, res) => {
      const search = searches[req.params[0]];
      const results = search.queries.map(({ id }) => [id, {
        error: [],
        execution_stats: {
          timestamp: '2019-07-05T13:37:00Z',
          effective_timerange: {
            from: '2019-07-04T13:37:00Z',
            to: '2019-07-05T13:37:00Z',
          },
        },
        search_types: {},
        state: 'COMPLETED',
      }])
        .reduce((prev, [id, result]) => ({ ...prev, [id]: result }), {});
      return res.json({
        id: `${req.params[0]}-job`,
        search_id: req.params[0],
        owner: 'betty',
        results,
        execution: {
          done: true,
          cancelled: false,
          completed_exceptionally: false,
        },
      });
    });

    const server = api.listen();
    const { port } = server.address();

    const url = `http://localhost:${port}${prefix}`;
    return jest.fn(() => url);
  }()),
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2DevMode: jest.fn(() => false),
}));
jest.mock('stores/sessions/SessionStore', () => ({
  isLoggedIn: jest.fn(() => true),
  getSessionId: jest.fn(() => 'foobar'),
}));

jest.mock('components/navigation/Navigation', () => 'navigation-bar');
jest.mock('routing/AppGlobalNotifications', () => 'app-global-notifications');

describe('Create a new view', () => {
  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, viewsBindings));
  });

  it('using Views Page', async () => {
    const { getByText, getAllByText } = render(<AppRouter />);
    history.push(Routes.VIEWS.LIST);

    await wait(() => getAllByText('Create new view'));
    const button = getAllByText('Create new view')[0];
    fireEvent.click(button);
    await wait(() => getByText('Query#1'));
    await wait(() => getByText('New View'));
  });
});
