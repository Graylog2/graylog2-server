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
import { Formik, Form } from 'formik';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor, act } from 'wrappedTestingLibrary';

import type { PasswordComplexityConfigType } from 'stores/configurations/ConfigurationsStore';
import { PASSWORD_SPECIAL_CHARACTERS } from 'logic/users/passwordComplexity';

import PasswordFormGroup, { validatePasswords } from './PasswordFormGroup';

const passwordComplexityConfig: PasswordComplexityConfigType = {
  min_length: 8,
  require_uppercase: true,
  require_lowercase: true,
  require_numbers: true,
  require_special_chars: true,
};

const renderPasswordFormGroup = () =>
  render(
    <Formik
      initialValues={{ password: '', password_repeat: '' }}
      onSubmit={jest.fn()}
      validate={(values) => {
        const { password, password_repeat: passwordRepeat } = values;

        return validatePasswords({}, password, passwordRepeat, passwordComplexityConfig);
      }}>
      <Form>
        <PasswordFormGroup passwordComplexityConfig={passwordComplexityConfig} />
      </Form>
    </Formik>,
  );

describe('<PasswordFormGroup />', () => {
  it('shows helper text before input', async () => {
    renderPasswordFormGroup();

    expect(screen.getByText('Password must be at least 8 characters long.', { exact: false })).toBeInTheDocument();
  });

  it('hides helper text once password is valid', async () => {
    renderPasswordFormGroup();

    const passwordInput = screen.getByPlaceholderText('Password');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(passwordInput, 'Abcdef1!');
    });

    await waitFor(() => {
      expect(
        screen.queryByText('Password must be at least 8 characters long.', { exact: false }),
      ).not.toBeInTheDocument();
    });
  });

  it('shows only unmet rules after blur', async () => {
    renderPasswordFormGroup();

    const passwordInput = screen.getByPlaceholderText('Password');
    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.type(passwordInput, 'abc');
      await userEvent.tab();
    });

    expect(screen.getByText('Password must be at least 8 characters long.', { exact: false })).toBeInTheDocument();
    expect(
      screen.getByText('Password must contain at least one uppercase letter.', { exact: false }),
    ).toBeInTheDocument();
    expect(screen.getByText('Password must contain at least one number.', { exact: false })).toBeInTheDocument();
    expect(
      screen.getByText(`Password must contain at least one special character from: ${PASSWORD_SPECIAL_CHARACTERS}`, {
        exact: false,
      }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText('Password must contain at least one lowercase letter.', { exact: false }),
    ).not.toBeInTheDocument();
  });
});
