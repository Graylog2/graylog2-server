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
import React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ContentPackListItem from './ContentPackListItem';

describe('<ContentPackListItem />', () => {
  const pack = {
    id: '14',
    name: 'SSH Archive',
    created_at: '2023-10-25T13:33:51.147Z',
    description: 'description',
    rev: 3,
    server_version: '6.0.0-SNAPSHOT',
    summary: 'The Open Thread Exchange Lookup Table of the Threat Intel Plugin',
    url: 'https://github.com/Graylog2/graylog2-server',
    v: 1,
    vendor: 'Graylog <hello@graylog.com>',
  };

  const metadata = {
    1: { 1: { installation_count: 1 } },
    2: { 5: { installation_count: 2 } },
  };

  it('render content pack item', async () => {
    render(
      <ContentPackListItem pack={pack}
                           contentPackMetadata={metadata}
                           onDeletePack={() => {}}
                           onInstall={() => {}} />);

    await screen.findByText('SSH Archive');
  });

  it('delete content pack item version', async () => {
    const deleteFn = jest.fn();

    render(
      <ContentPackListItem pack={pack}
                           contentPackMetadata={metadata}
                           onDeletePack={deleteFn}
                           onInstall={() => {}} />);

    userEvent.click(await screen.findByRole('button', { name: /more actions/i }));
    userEvent.click((await screen.findAllByRole('menuitem', { name: 'Delete All Versions' }))[0]);

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: /delete all versions/i })).not.toBeInTheDocument();
    });

    expect(deleteFn).toHaveBeenCalledTimes(1);
  });

  it('install content pack item version', async () => {
    const deleteFn = jest.fn();

    render(
      <ContentPackListItem pack={pack}
                           contentPackMetadata={metadata}
                           onDeletePack={deleteFn}
                           onInstall={() => {}} />);

    userEvent.click(await screen.findByRole('button', { name: /more actions/i }));
    userEvent.click((await screen.findAllByRole('menuitem', { name: 'Delete All Versions' }))[0]);

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: /delete all versions/i })).not.toBeInTheDocument();
    });

    expect(deleteFn).toHaveBeenCalledTimes(1);
  });
});
