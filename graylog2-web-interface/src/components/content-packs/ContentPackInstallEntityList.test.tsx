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
import { render, screen } from 'wrappedTestingLibrary';

import ContentPackInstallEntityList from './ContentPackInstallEntityList';

describe('<ContentPackInstallEntityList />', () => {
  const entities = [
    { id: '5bdc4f4b3d27461ce46816cf', type: { name: 'lookup_cache', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca80', title: 'Threat Intel Uncached Adapters' },
    { id: '5bdc4f4b3d27461ce46816d4', type: { name: 'lookup_table', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca8c', title: 'Tor Exit Node List' },
    { id: '5bdc4f4b3d27461ce46816d1', type: { name: 'lookup_adapter', version: '1' }, content_pack_entity_id: '5ac762873d274666e34eca87', title: 'Tor Exit Node' },
  ];

  it('should render without entities', async () => {
    render(<ContentPackInstallEntityList />);

    await screen.findByText(/loading/i);
  });

  it('should render with entities', async () => {
    render(<ContentPackInstallEntityList entities={entities} />);

    await screen.findByText(/Installed Entities/i);
    await screen.findByText('Threat Intel Uncached Adapters');
    await screen.findByText('Tor Exit Node List');
    await screen.findByText('Tor Exit Node');
  });

  it('should render with empty entities', async () => {
    render(<ContentPackInstallEntityList entities={[]} />);

    await screen.findByText(/Installed Entities/i);
  });

  it('should render with uninstall and entities', async () => {
    render(<ContentPackInstallEntityList uninstall entities={entities} />);

    await screen.findByText(/Entites to be uninstalled/i);
    await screen.findByText('Threat Intel Uncached Adapters');
    await screen.findByText('Tor Exit Node List');
    await screen.findByText('Tor Exit Node');
  });
});
