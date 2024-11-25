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

import ContentPackParameters from 'components/content-packs/ContentPackParameters';
import ContentPack from 'logic/content-packs/ContentPack';

jest.mock('logic/generateId', () => jest.fn(() => 'dead-beef'));

describe('<ContentPackParameters />', () => {
  it('should render with empty parameters', async () => {
    const contentPack = {
      parameters: [],
      entities: [],
    };
    render(<ContentPackParameters contentPack={contentPack} appliedParameter={{}} />);

    await screen.findByRole('heading', { name: /parameters list/i });
  });

  it('should render a parameter', async () => {
    const entity = {
      id: '111-beef',
      v: '1.0',
      type: {
        name: 'input',
        version: '1',
      },
      data: {
        name: { '@type': 'string', '@value': 'Input' },
        title: { '@type': 'string', '@value': 'A good input' },
        configuration: {
          listen_address: { '@type': 'string', '@value': '1.2.3.4' },
          port: { '@type': 'integer', '@value': '23' },
        },
      },
    };
    const contentPack = ContentPack.builder()
      .parameters(
        [{
          name: 'A parameter name',
          title: 'A parameter title',
          description: 'A parameter descriptions',
          type: 'string',
          default_value: 'test',
        }],
      )
      .entities([entity])
      .build();
    render(<ContentPackParameters contentPack={contentPack} appliedParameter={{}} />);

    await screen.findByRole('heading', { name: /parameters list/i });
  });
});
