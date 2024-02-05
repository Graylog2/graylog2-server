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
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ContentPacksList from 'components/content-packs/ContentPacksList';

import type { ContentPackInstallation, ContentPackMetadata } from './Types';

describe('<ContentPacksList />', () => {
  const contentPacks = [
    { id: '1', rev: 1, name: 'UFW Grok Patterns', summary: 'Content Pack: Grok Patterns to extract informations from UFW logfiles', server_version: '1.0' },
    { id: '2', rev: 1, name: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', server_version: '2.1' },
    { id: '3', rev: 1, name: 'Backup Content Pack', summary: '', server_version: '3.0' },
    { id: '4', rev: 1, name: 'SSH Archive', summary: 'A crypted backup over ssh.', server_version: '3.4' },
    { id: '5', rev: 1, name: 'FTP Backup', summary: 'Fast but insecure backup', server_version: '1.0' },
    { id: '6', rev: 1, name: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', server_version: '1.0' },
    { id: '7', rev: 1, name: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', server_version: '2.1' },
    { id: '8', rev: 1, name: 'Backup Content Pack', summary: '', server_version: '3.0', states: ['installed'] },
    { id: '9', rev: 1, name: 'SSH Archive', summary: 'A crypted backup over ssh.', server_version: '3.4' },
    { id: '10', rev: 1, name: 'FTP Backup', summary: 'Fast but insecure backup', server_version: '1.0' },
    { id: '11', rev: 1, name: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', server_version: '1.0' },
    { id: '12', rev: 1, name: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', server_version: '2.1' },
    { id: '13', rev: 1, name: 'Backup Content Pack', summary: '', server_version: '3.0' },
    { id: '14', rev: 1, name: 'SSH Archive', summary: 'A crypted backup over ssh.', server_version: '3.4' },
    { id: '15', rev: 1, name: 'FTP Backup', summary: 'Fast but insecure backup', server_version: '1.0' },
  ] as Array<ContentPackInstallation>;

  it('should render with empty content packs', async () => {
    render(<ContentPacksList contentPacks={[]} />);

    await screen.findByText('No content packs found. Please create or upload one');
  });

  it('should render with content packs', async () => {
    const metadata = {
      1: { 1: { installation_count: 1 } },
      2: { 5: { installation_count: 2 } },
    } as ContentPackMetadata;
    render(<ContentPacksList contentPacks={contentPacks} contentPackMetadata={metadata} />);

    await screen.findByText('Content Pack: Grok Patterns to extract informations from UFW logfiles');
  });

  it('should do pagination', async () => {
    render(<ContentPacksList contentPacks={contentPacks} />);

    expect(await screen.findAllByText(/Latest Version:/)).toHaveLength(10);

    userEvent.click((await screen.findAllByRole('button', { name: /open page 2/i }))[0]);

    const activePage = (await screen.findAllByTitle('Active page'))[0];

    expect(within(activePage).getByText(2)).toBeInTheDocument();
    expect(await screen.findAllByText(/Latest Version:/)).toHaveLength(5);
  });

  it('should delete a content pack', async () => {
    const deleteFn = jest.fn();
    render(<ContentPacksList contentPacks={contentPacks} onDeletePack={deleteFn} />);

    userEvent.click((await screen.findAllByRole('button', { name: /more actions/i }))[0]);

    userEvent.click(await screen.findByRole('menuitem', { name: 'Delete All Versions' }));

    expect(deleteFn).toHaveBeenCalledTimes(1);
  });
});
