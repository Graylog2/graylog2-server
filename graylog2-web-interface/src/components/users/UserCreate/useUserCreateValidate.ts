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
import { PluginStore } from 'graylog-web-plugin/plugin';

import AppConfig from 'util/AppConfig';
import usePasswordComplexityConfig from 'components/users/usePasswordComplexityConfig';

import { validatePasswords } from './PasswordFormGroup';

const isCloud = AppConfig.isCloud();
const oktaUserForm = isCloud ? PluginStore.exports('cloud')[0].oktaUserForm : null;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type FormValues = Record<string, any>;

const useUserCreateValidate = () => {
  const passwordComplexityConfig = usePasswordComplexityConfig();

  return (values: FormValues) => {
    let errors = {};
    const { password, password_repeat: passwordRepeat, roles } = values as {
      password: string;
      password_repeat: string;
      roles: string[];
    };

    if (isCloud && oktaUserForm) {
      const {
        validations: { password: validateCloudPasswords },
      } = oktaUserForm;

      errors = validateCloudPasswords(errors, password, passwordRepeat);
    } else {
      errors = validatePasswords(errors, password, passwordRepeat, passwordComplexityConfig);
    }

    if (!roles?.some((r) => r === 'Reader' || r === 'Admin')) {
      errors = { ...errors, roles: 'You need to select at least one of the Reader or Admin roles.' };
    }

    return errors;
  };
};

export default useUserCreateValidate;
