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

import { asMock } from 'helpers/mocking';
import AppConfig from 'util/AppConfig';

import LoginChrome from './LoginChrome';

jest.mock('components/login/PlasmaBackground', () => () => <span>Plasma effect</span>);

const customSvg = '<svg xmlns="http://www.w3.org/2000/svg"><rect width="10" height="10" /></svg>';

describe('LoginChrome', () => {
  beforeEach(() => {
    asMock(AppConfig.branding).mockReturnValue(undefined);
  });

  it('shows the plasma effect for the default branding', async () => {
    render(<LoginChrome>Login form</LoginChrome>);

    await screen.findByText('Plasma effect');
  });

  it('does not show the plasma effect when a custom login background is set', async () => {
    asMock(AppConfig.branding).mockReturnValue({ login: { background: customSvg } });

    render(<LoginChrome>Login form</LoginChrome>);

    await screen.findByText('Login form');

    expect(screen.queryByText('Plasma effect')).not.toBeInTheDocument();
  });

  it('does not show the plasma effect when a custom logo is set', async () => {
    asMock(AppConfig.branding).mockReturnValue({ logo: { light: customSvg, dark: customSvg } });

    render(<LoginChrome>Login form</LoginChrome>);

    await screen.findByText('Login form');

    expect(screen.queryByText('Plasma effect')).not.toBeInTheDocument();
  });
});
