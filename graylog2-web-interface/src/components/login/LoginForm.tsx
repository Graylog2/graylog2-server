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
import { useMutation } from '@tanstack/react-query';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { ModalSubmit } from 'components/common';
import { Input } from 'components/bootstrap';
import { SessionActions } from 'stores/sessions/SessionStore';
import type FetchError from 'logic/errors/FetchError';

type Props = {
  onErrorChange: (message?: string) => void,
};

const SigninButton = styled(ModalSubmit)(({ theme }) => css`
  button.mantine-Button-root {
    background-color: ${theme.colors.brand.primary};
    border-color: ${theme.colors.brand.primary};
    
    &:hover {
      background-color: ${theme.colors.brand.primary};
      border-color: ${theme.colors.brand.primary};
    }
  }
`);

const performLogin = ([username, password, host]: [string, string, string]) => SessionActions.login(username, password, host);

const LoginForm = ({ onErrorChange }: Props) => {
  const { mutateAsync: login, isLoading } = useMutation(performLogin,
    {
      onError: (error: FetchError) => {
        if (error.additional.status === 401) {
          onErrorChange('Invalid credentials, please verify them and retry.');
        } else {
          onErrorChange(`Error - the server returned: ${error.additional.status} - ${error.message}`);
        }
      },
    });

  const onSignInClicked = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onErrorChange();
    const formData = new FormData(event.currentTarget);
    const username = formData.get('username') as string;
    const password = formData.get('password') as string;
    const location = document.location.host;

    return login([username, password, location]);
  };

  return (
    <form onSubmit={onSignInClicked}>
      <Input id="username"
             type="text"
             label="Username"
             autoFocus
             required />

      <Input id="password"
             type="password"
             label="Password"
             required />

      <SigninButton displayCancel={false}
                    isSubmitting={isLoading}
                    isAsyncSubmit
                    submitLoadingText="Signing in..."
                    submitButtonText="Sign in" />
    </form>
  );
};

LoginForm.propTypes = {
  onErrorChange: PropTypes.func.isRequired,
};

export default LoginForm;
