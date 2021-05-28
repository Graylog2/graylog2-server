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
import formidableMiddleware from 'express-formidable';
import FormData from 'form-data';

import fetch, { Builder, fetchFile } from './FetchProvider';

jest.unmock('./FetchProvider');

jest.mock('injection/StoreProvider', () => ({
  getStore: (key) => ({
    Session: {
      isLoggedIn: jest.fn(() => true),
      getSessionId: jest.fn(() => 'foobar'),
    },
  }[key]),
}));

const mockLogout = jest.fn();

jest.mock('injection/ActionsProvider', () => ({
  getActions: (key) => ({
    Session: {
      logout: mockLogout,
    },
    ServerAvailability: {
      reportSuccess: jest.fn(),
      reportError: jest.fn(),
    },
  }[key]),
}));

const PORT = 0;

const setUpServer = () => {
  const app = express();

  app.use(formidableMiddleware());
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  app.use((err, req, res, next) => console.error(err));

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

  app.post('/failIfWrongAcceptHeader', (req, res) => {
    if (req.accepts().includes('text/csv')) {
      res.send('foo,bar,baz');
    } else {
      res.status(500).end();
    }
  });

  app.get('/simulatesSessionExpiration', (req, res) => {
    res.status(401).end();
  });

  app.put('/uploadFile', (req, res) => {
    const contentType = req.header('Content-Type');

    if (contentType === 'application/json') {
      res.status(400).send('Invalid Content-Type set for form data!').end();

      return;
    }

    res.send(req.fields).end();
  });

  app.post('/errorWithMessage', (req, res) => res.status(500).send({ message: 'The dungeon collapses. You die!' }));

  return app.listen(PORT, () => {});
};

describe('FetchProvider', () => {
  let server: ReturnType<typeof setUpServer>;
  let baseUrl;

  beforeAll(() => {
    server = setUpServer();
    // eslint-disable-next-line global-require
    window.fetch = nodeFetch;

    // @ts-ignore Types do not match actual result for some reason
    const { port } = server.address();
    baseUrl = `http://localhost:${port}`;
  });

  afterAll(() => {
    server.close();
  });

  it.each([
    ['GET with json', 'GET', 'test1', { text: 'test' }],
    ['POST with json', 'POST', 'test2', { text: 'test' }],
    ['POST with text', 'POST', 'test3', 'uuid-beef-feed'],
    ['POST without content', 'POST', 'test4', null],
    ['DELETE without content and status 204', 'DELETE', 'test5', null],
  ])('should receive a %s', async (text, method, url, expectedResponse) => {
    return fetch(method, `${baseUrl}/${url}`).then((response) => {
      expect(response).toStrictEqual(expectedResponse);
    });
  });

  it('sets correct accept header', async () => {
    const result = await fetchFile('POST', `${baseUrl}/failIfWrongAcceptHeader`, {}, 'text/csv');

    expect(result).toEqual('foo,bar,baz');
  });

  it('removes local session if 401 is returned', async () => {
    const error = await fetch('GET', `${baseUrl}/simulatesSessionExpiration`).catch((e) => e);

    expect(error.name).toEqual('FetchError');
    expect(error.message).toEqual('There was an error fetching a resource: Unauthorized. Additional information: Not available');

    expect(mockLogout).toHaveBeenCalledWith('foobar');
  });

  it('supports uploading form data without content type', async () => {
    const form = new FormData();
    form.append('foo', 'bar');
    const builder = new Builder('PUT', `${baseUrl}/uploadFile`).formData(form);
    const result = await builder.build();

    expect(result).toEqual({ foo: 'bar' });
  });

  it('extracts the error message from a failed request', async () => {
    await expect(fetch('POST', `${baseUrl}/errorWithMessage`))
      .rejects
      .toThrowError('There was an error fetching a resource: Internal Server Error. Additional information: The dungeon collapses. You die!');
  });

  it('handles error properly when endpoint is not reachable', async () => {
    const error = await fetch('POST', 'http://localhost:12223/').catch((e) => e);

    expect(error.status).toEqual(undefined);
    expect(error.message).toEqual('There was an error fetching a resource: undefined. Additional information: Not available');
  });
});
