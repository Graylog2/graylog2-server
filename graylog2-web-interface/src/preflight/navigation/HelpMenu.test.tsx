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
import * as React from 'react';
import userEvent from '@testing-library/user-event';
import { renderPreflight, screen } from 'wrappedTestingLibrary';

import HelpMenu from 'preflight/navigation/HelpMenu';

describe('HelpMenu', () => {
  it('shows external help links when the menu is opened', async () => {
    renderPreflight(<HelpMenu />);

    await userEvent.click(await screen.findByRole('button', { name: /get help/i }));

    const documentationLink = await screen.findByRole('menuitem', { name: /documentation/i });

    expect(documentationLink).toHaveAttribute('href', 'https://go2docs.graylog.org/current');
    expect(documentationLink).toHaveAttribute('target', '_blank');
    expect(documentationLink).toHaveAttribute('rel', 'noopener noreferrer');
  });
});
