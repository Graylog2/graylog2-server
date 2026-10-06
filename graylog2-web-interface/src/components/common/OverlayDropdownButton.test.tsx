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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { MenuItem } from 'components/bootstrap';

import OverlayDropdownButton from './OverlayDropdownButton';

describe('OverlayDropdownButton', () => {
  it('renders the button and opens the menu on click', async () => {
    render(
      <OverlayDropdownButton title="More" buttonTitle="More actions" bsSize="xsmall">
        <MenuItem>Inspect</MenuItem>
      </OverlayDropdownButton>,
    );

    const button = screen.getByRole('button', { name: 'More actions' });

    expect(button).toBeInTheDocument();

    await userEvent.click(button);

    expect(await screen.findByText('Inspect')).toBeInTheDocument();
  });
});
