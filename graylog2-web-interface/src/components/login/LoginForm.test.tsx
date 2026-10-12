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
import userEvent from '@testing-library/user-event';

import LoginForm from './LoginForm';

const mockLogin = jest.fn();

jest.mock('./useLogin', () => () => ({
  login: mockLogin,
  isLoading: false,
}));

describe('LoginForm', () => {
  beforeEach(() => {
    mockLogin.mockClear();
    mockLogin.mockResolvedValue(undefined);
  });

  it('trims leading and trailing whitespace from the username before submitting', async () => {
    render(<LoginForm onErrorChange={() => {}} />);

    await userEvent.type(screen.getByLabelText('Username'), '  alice  ');
    await userEvent.type(screen.getByLabelText('Password'), 'secret');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(mockLogin).toHaveBeenCalledWith('alice', 'secret', expect.any(String));
  });

  it('does not modify a username that has no surrounding whitespace', async () => {
    render(<LoginForm onErrorChange={() => {}} />);

    await userEvent.type(screen.getByLabelText('Username'), 'bob');
    await userEvent.type(screen.getByLabelText('Password'), 'secret');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(mockLogin).toHaveBeenCalledWith('bob', 'secret', expect.any(String));
  });

  it('does not trim whitespace inside the password', async () => {
    render(<LoginForm onErrorChange={() => {}} />);

    await userEvent.type(screen.getByLabelText('Username'), ' alice ');
    await userEvent.type(screen.getByLabelText('Password'), '  pa ss  ');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(mockLogin).toHaveBeenCalledWith('alice', '  pa ss  ', expect.any(String));
  });
});
