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
import 'helpers/mocking/react-dom_mock';

import ContentPackInstallations from 'components/content-packs/ContentPackInstallations';

describe('<ContentPackInstallations />', () => {
  const installations = [
    {
      _id: '5b55b8b73d274645e49f7eec',
      content_pack_id: '82d59d98-7440-ab74-9857-36b2f1b7ab8a',
      content_pack_revision: 1,
      parameters: {
        SOURCE: {
          name: 'string',
          value: 'hulud.com.uk',
        },
      },
      entities: [
        {
          id: '5ba38df33d274660f0b94118',
          type: {
            name: 'input',
            version: '1',
          },
          content_pack_entity_id: '5b6d973d3d274645572a4318',
          title: 'hulud.net',
        },
      ],
      comment: 'The fake input',
      created_at: '2018-07-23T11:15:03.871Z',
      created_by: 'kmerz',
    },
  ];

  it('should render without installations', async () => {
    render(<ContentPackInstallations />);

    await screen.findByText('No data available.');
  });

  it('should render with empty installations', async () => {
    render(<ContentPackInstallations installations={[]} />);

    await screen.findByText('No data available.');
  });

  it('should render with a installation', async () => {
    render(<ContentPackInstallations installations={installations} />);

    await screen.findByText('The fake input');
  });

  it('should uninstall a installation', async () => {
    const uninstallFn = jest.fn((contentPackId, installId) => {
      expect(contentPackId).toEqual(installations[0].content_pack_id);
      expect(installId).toEqual(installations[0]._id);
    });
    render(<ContentPackInstallations installations={installations} onUninstall={uninstallFn} />);

    const uninstall = await screen.findByRole('button', { name: 'Uninstall' });

    uninstall.click();

    expect(uninstallFn).toHaveBeenCalled();
  });
});
