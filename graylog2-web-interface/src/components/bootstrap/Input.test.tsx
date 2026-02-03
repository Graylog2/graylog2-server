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

import { Button } from 'components/bootstrap';

import Input from './Input';

describe('Input', () => {
  it('renders a button after the input if buttonAfter is passed', async () => {
    render(<Input id="inputWithButton" type="text" buttonAfter={<Button />} />);

    await screen.findByRole('button');
  });

  it('renders a addon after the input if addonAfter is passed', async () => {
    render(<Input id="inputWithAddon" type="text" addonAfter=".00" />);
    await screen.findByText('.00');
  });

  it('renders a checkbox addon after the input if addonAfter is passed', async () => {
    render(
      <Input
        id="inputWithCheckboxAddon"
        type="text"
        addonAfter={<input id="addonCheckbox" type="checkbox" aria-label="..." />}
      />,
    );

    await screen.findByRole('checkbox');
  });

  it('renders input w/ `name` attribute w/o setting prop', async () => {
    render(<Input id="inputWithoutNameProp" type="text" />);

    expect(await screen.findByRole('textbox')).toHaveProperty('name', 'inputWithoutNameProp');
  });

  it('renders input w/ `name` attribute w/ setting prop', async () => {
    render(<Input id="inputWithoutNameProp" name="inputWithNameProp" type="text" />);

    expect(await screen.findByRole('textbox')).toHaveProperty('name', 'inputWithNameProp');
  });

  it('renders input w/ provided error', async () => {
    render(<Input id="inputWithError" type="text" error="The error message" />);

    await screen.findByText('The error message');
  });

  it('renders input w/ provided help', async () => {
    render(<Input id="inputWithHelp" type="text" help="The help text" />);

    await screen.findByText('The help text');
  });

  it('renders input w/ provided help and error', async () => {
    render(<Input id="inputWithHelp" type="text" help="The help text" error="The error message" />);

    await screen.findByText('The error message');
    await screen.findByText('The help text');
  });
});
