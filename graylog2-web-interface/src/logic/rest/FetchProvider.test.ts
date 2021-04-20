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
import express from 'express';
import nodeFetch from 'node-fetch';

import fetch from './FetchProvider';

jest.unmock('./FetchProvider');

jest.mock('stores/sessions/SessionStore', () => ({
  isLoggedIn: jest.fn(() => true),
  getSessionId: jest.fn(() => 'foobar'),
}));

const PORT = 0;

const setUpServer = () => {
  const app = express();

  app.get('/test1', (req, res) => {
    res.send({ text: 'test' });
  });

  app.post('/test2', (req, res) => {
    res.send({ text: 'test' });
  });

  app.post('/test3', (req, res) => {
    res.send('"uuid-beef-feed"');
  });

  app.post('/test4', (req, res) => {
    res.send(undefined);
  });

  app.delete('/test5', (req, res) => {
    res.status(204).end();
  });


  return app.listen(PORT, () => {});
};

describe('FetchProvider', () => {
  let server;

  beforeAll(() => {
    server = setUpServer();
    // eslint-disable-next-line global-require
    window.fetch = nodeFetch;
  });

  afterAll(() => {
    server.close();
  });

  it.each([
    ['a GET with json', 'GET', 'test1', { text: 'test' }],
    ['a POST with json', 'POST', 'test2', { text: 'test' }],
    ['a POST with text', 'POST', 'test3', 'uuid-beef-feed'],
    ['a POST without content', 'POST', 'test4', null],
    ['a DELETE without content and status 204', 'DELETE', 'test5', null],
  ])('should receive a %s', async (text, method, url, expectedResponse) => {
    return fetch(method, `http://localhost:${server.address().port}/${url}`, undefined).then((response) => {
      expect(response).toStrictEqual(expectedResponse);
    });
  });
});
