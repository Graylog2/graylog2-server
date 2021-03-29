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
const JSDomEnvironment = require('jest-environment-jsdom');
const express = require('express');
const cors = require('cors');

class IntegrationEnvironment extends JSDomEnvironment {
  constructor(config) {
    super(config);

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
        errors: [],
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

    this.server = api.listen();
    const { port } = this.server.address();

    this.global.api_url = `http://localhost:${port}${prefix}`;

    // eslint-disable-next-line global-require
    this.global.window.fetch = require('node-fetch');
  }

  async setup() {
    await super.setup();
  }

  async teardown() {
    await super.teardown();
    this.server.close();
  }

  runScript(script) {
    return super.runScript(script);
  }
}

module.exports = IntegrationEnvironment;
