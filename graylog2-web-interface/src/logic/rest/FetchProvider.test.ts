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

import ErrorsActions from 'actions/errors/ErrorsActions';
import { asMock } from 'helpers/mocking';

import fetch, { Builder, fetchFile } from './FetchProvider';

jest.unmock('./FetchProvider');
const mockLogout = jest.fn();

jest.mock('stores/sessions/SessionStore', () => ({
  SessionStore: {
    isLoggedIn: jest.fn(() => true),
  },
  SessionActions: {
    logout: mockLogout,
  },
}));

jest.mock('stores/sessions/ServerAvailabilityStore', () => ({
  ServerAvailabilityActions: {
    reportSuccess: jest.fn(),
    reportError: jest.fn(),
  },
}));

jest.mock('actions/errors/ErrorsActions', () => ({
  report: jest.fn(),
}));

const PORT = 0;

const setUpServer = () => {
  const app = express();

  app.use(formidableMiddleware());
  // eslint-disable-next-line @typescript-eslint/no-unused-vars,no-console
  app.use((err, _req, _res, _next) => console.error(err));

  app.get('/test1', (_req, res) => {
    res.send({ text: 'test' });
  });

  app.post('/test2', (_req, res) => {
    res.send({ text: 'test' });
  });

  app.post('/test3', (_req, res) => {
    res.send('"uuid-beef-feed"');
  });

  app.post('/test4', (_req, res) => {
    res.send(undefined);
  });

  app.delete('/test5', (_req, res) => {
    res.status(204).end();
  });

  app.post('/failIfWrongAcceptHeader', (req, res) => {
    if (req.accepts().includes('text/csv')) {
      res.send('foo,bar,baz');
    } else {
      res.status(500).end();
    }
  });

  app.get('/simulatesSessionExpiration', (_req, res) => {
    res.status(401).end();
  });

  app.get('/simulatesUnauthorized', (_req, res) => {
    res.status(403).end();
  });

  app.put('/uploadFile', (req, res) => {
    const contentType = req.header('Content-Type');

    if (contentType === 'application/json') {
      res.status(400).send('Invalid Content-Type set for form data!').end();

      return;
    }

    res.send(req.fields).end();
  });

  app.post('/errorWithMessage', (_req, res) => res.status(500).send({ message: 'The dungeon collapses. You die!' }));

  return app.listen(PORT, () => {});
};

describe('FetchProvider', () => {
  let server: ReturnType<typeof setUpServer>;
  let baseUrl;

  beforeAll(() => {
    server = setUpServer();

    // @ts-expect-error Node.js implementation has slightly differing types
    window.fetch = nodeFetch;

    // @ts-ignore Types do not match actual result for some reason
    const { port } = server.address();
    baseUrl = `http://localhost:${port}`;
  });

  beforeEach(() => {
    asMock(ErrorsActions.report).mockClear();
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
  ])('should receive a %s', async (_text, method, url, expectedResponse) => {
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

    expect(mockLogout).toHaveBeenCalled();
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

  it('reports 403 by default', async () => {
    const promise = new Builder('GET', `${baseUrl}/simulatesUnauthorized`)
      .json()
      .build();
    const error = await promise.catch((e) => e);

    expect(error.status).toEqual(403);
    expect(error.message).toEqual('There was an error fetching a resource: Forbidden. Additional information: Not available');

    expect(ErrorsActions.report).toHaveBeenCalledWith(expect.objectContaining({
      type: 'UnauthorizedError',
    }));
  });

  it('allows ignoring 403', async () => {
    const promise = new Builder('GET', `${baseUrl}/simulatesUnauthorized`)
      .json()
      .ignoreUnauthorized()
      .build();
    const error = await promise.catch((e) => e);

    expect(error.message).toEqual('There was an error fetching a resource: Forbidden. Additional information: Not available');

    expect(ErrorsActions.report).not.toHaveBeenCalled();
  });
});
