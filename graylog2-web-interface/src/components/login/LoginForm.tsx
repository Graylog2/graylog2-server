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
import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { ModalSubmit } from 'components/common';
import { Input } from 'components/bootstrap';
import { SessionActions } from 'stores/sessions/SessionStore';

type Props = {
  onErrorChange: (message?: string) => void,
};

const SigninButton = styled(ModalSubmit)(({ theme }) => css`
  button.btn-success {
    background-color: ${theme.colors.brand.primary};
    border-color: ${theme.colors.brand.primary};
    
    :hover {
      background-color: ${theme.colors.brand.primary};
      border-color: ${theme.colors.brand.primary};
    }
  }
`);

const LoginForm = ({ onErrorChange }: Props) => {
  const [isLoading, setIsLoading] = useState(false);
  let promise;
  let usernameInput;
  let passwordInput;

  useEffect(() => {
    return () => {
      if (promise) {
        promise.cancel();
      }
    };
  }, [promise]);

  const onSignInClicked = (event) => {
    event.preventDefault();
    onErrorChange();
    setIsLoading(true);
    const username = usernameInput.getValue();
    const password = passwordInput.getValue();
    const location = document.location.host;

    promise = SessionActions.login(username, password, location);

    promise.catch((error) => {
      if (error.additional.status === 401) {
        onErrorChange('Invalid credentials, please verify them and retry.');
      } else {
        onErrorChange(`Error - the server returned: ${error.additional.status} - ${error.message}`);
      }
    });

    promise.finally(() => {
      if (!promise.isCancelled()) {
        setIsLoading(false);
      }
    });
  };

  return (
    <form onSubmit={onSignInClicked}>
      <Input ref={(username) => { usernameInput = username; }}
             id="username"
             type="text"
             label="Username"
             autoFocus
             required />

      <Input ref={(password) => { passwordInput = password; }}
             id="password"
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
