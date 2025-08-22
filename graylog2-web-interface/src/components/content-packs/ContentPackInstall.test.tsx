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
import React, { act } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { StoreMock as MockStore } from 'helpers/mocking';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: { prepare: async () => {} },
  EntityShareStore: MockStore(['getInitialState', () => ({})]),
}));

describe('<ContentPackInstall />', () => {
  const parameter = {
    title: 'A parameter',
    name: 'PARAM',
    type: 'string',
    default_value: 'parameter',
  };

  const entity = {
    id: '1',
    v: '1',
    type: {
      name: 'grok_pattern',
      version: '1',
    },
    data: {
      title: { '@type': 'string', '@value': 'franz' },
      descr: { '@type': 'string', '@value': 'hans' },
    },
    toJSON: () => ({}),
    constraints: [],
  };

  const contentPack = {
    id: '1',
    v: '1',
    rev: 2,
    name: 'UFW Grok Patterns',
    description: 'Grok Patterns to extract informations from UFW logfiles',
    summary: 'This is a summary',
    url: 'www.graylog.com',
    vendor: 'graylog.com',
    parameters: [parameter],
    entities: [entity],
  };

  it('should render a install', async () => {
    render(<ContentPackInstall contentPack={contentPack} />);

    await screen.findByText(/install comment/i);
  });

  it('should call install when called', async () => {
    const installFn = jest.fn((id, rev, param) => {
      expect(id).toBe('1');
      expect(rev).toBe(2);
      expect(param).toEqual({ comment: 'Test', parameters: { PARAM: { '@type': 'string', '@value': 'parameter' } } });
    });
    let instance = null;

    render(
      <ContentPackInstall
        ref={(_instance) => {
          instance = _instance;
        }}
        contentPack={contentPack}
        onInstall={installFn}
      />,
    );
    await screen.findByText(/install comment/i);

    const commentInput = await screen.findByRole('textbox', { name: /comment/i });
    await userEvent.type(commentInput, 'Test');

    act(() => {
      (instance as ContentPackInstall).onInstall();
    });

    expect(installFn.mock.calls.length).toBe(1);
  });

  it('should not call install when parameter is missing', async () => {
    const installFn = jest.fn();
    let instance = null;

    render(
      <ContentPackInstall
        ref={(_instance) => {
          instance = _instance;
        }}
        contentPack={contentPack}
        onInstall={installFn}
      />,
    );

    await screen.findByText(/install comment/i);

    const commentInput = await screen.findByRole('textbox', { name: /parameter/i });
    await userEvent.clear(commentInput);

    act(() => {
      (instance as ContentPackInstall).onInstall();
    });

    expect(installFn.mock.calls.length).toBe(0);
  });
});
