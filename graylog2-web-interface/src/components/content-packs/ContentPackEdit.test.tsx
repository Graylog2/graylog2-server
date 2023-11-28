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

import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';

describe('<ContentPackEdit />', () => {
  const emptyContentPack = ContentPack.builder()
    .id('9950ba5a-0887-40a9-2b8f-8b50292cc7fb')
    .build();

  const enrichedEntity = {
    id: '111-beef',
    v: '1.0',
    type: 'dashboard',
    title: 'A good input',
    data: {
      name: 'Input',
      title: 'A good input',
      configuration: {
        listen_address: '1.2.3.4',
        port: '23',
      },
    },
  };
  const serverEntities = {
    dashboard: [{ id: '111-beef', type: 'dashboard', title: 'A good input' }],
  };
  const appliedParameter = { '111-beef': [{ configKey: 'configuration.port', paramName: 'PORT' }] };
  const selectedEntities = { dashboard: [{ id: '111-beef', type: 'dashbaord' }] };

  const parameter = { title: 'Port', name: 'PORT', type: 'integer', default_value: '23' };
  const filledContentPack = ContentPack.builder()
    .id('9950ba5a-0887-40a9-2b8f-8b50292cc7fb')
    .name('Content Pack the movie')
    .description('## No one dares')
    .summary('A old content pack')
    .vendor('Beinstein')
    .url('http://beinstein.com')
    .entities([enrichedEntity])
    .parameters([parameter])
    .build();

  it('should render spinner with no content pack', async () => {
    render(<ContentPackEdit />);

    await screen.findByText(/loading/i);
  });

  it('should render empty content pack for create', async () => {
    render(<ContentPackEdit contentPack={emptyContentPack}
                            selectedEntities={{}}
                            appliedParameter={{}}
                            entityIndex={{}} />);

    await screen.findByText('Content Selection');
  });

  it('should render with content pack for edit', async () => {
    render(
      <ContentPackEdit contentPack={filledContentPack}
                       appliedParameter={appliedParameter}
                       entityIndex={serverEntities}
                       selectedEntities={selectedEntities} />,
    );

    await screen.findByText('Content Selection');
  });

  it('should create a new content pack', async () => {
    const saveFn = jest.fn();

    render(
      <ContentPackEdit contentPack={filledContentPack}
                       appliedParameter={appliedParameter}
                       entityIndex={serverEntities}
                       onSave={saveFn}
                       selectedEntities={selectedEntities} />,
    );

    (await screen.findByRole('button', { name: 'Next' })).click();

    await screen.findByText(/Parameters list/i);

    (await screen.findByRole('button', { name: 'Next' })).click();

    (await screen.findByRole('button', { name: 'Create' })).click();

    await waitFor(() => {
      expect(saveFn).toHaveBeenCalled();
    });
  });
});
