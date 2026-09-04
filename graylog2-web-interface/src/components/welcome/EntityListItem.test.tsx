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

import EntityListItem from 'components/welcome/EntityListItem';
import { createGRN } from 'logic/permissions/GRN';

const grn = createGRN('dashboard', '1');

describe('EntityListItem', () => {
  it('Show type', async () => {
    render(<EntityListItem grn={grn} title="Title 1" />);

    await screen.findByText('dashboard');
  });

  it('Show correct link', async () => {
    render(<EntityListItem grn={grn} title="Title 1" />);

    const link = await screen.findByText('Title 1');

    expect(link).toHaveAttribute('href', '/dashboards/1');
  });
});
