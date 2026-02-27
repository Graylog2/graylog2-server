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
import { useField, useFormikContext } from 'formik';

import { passwordComplexityErrors, passwordComplexityHelpLines } from 'logic/users/passwordComplexity';
import type { PasswordComplexityConfigType } from 'stores/configurations/ConfigurationsStore';
import PasswordRules from 'components/users/PasswordRules';

type Options = {
  fieldName?: string;
  passwordComplexityConfig: PasswordComplexityConfigType;
};

const usePasswordHelpText = ({ fieldName = 'password', passwordComplexityConfig }: Options) => {
  const [{ value }, meta] = useField(fieldName);
  const { validateOnChange = true } = useFormikContext();
  const passwordValue = typeof value === 'string' ? value : '';
  const hasPassword = passwordValue.length > 0;
  const complexityErrors = passwordComplexityErrors(passwordValue, passwordComplexityConfig);
  const hideHelpText = hasPassword && complexityErrors.length === 0;
  const showPasswordError = validateOnChange ? !!(meta.error && meta.touched) : !!meta.error;
  const helpLines = hasPassword ? complexityErrors : passwordComplexityHelpLines(passwordComplexityConfig);
  const helpText: React.ReactElement | string | undefined =
    helpLines.length > 0 ? <PasswordRules lines={helpLines} /> : undefined;

  return showPasswordError || hideHelpText ? undefined : helpText;
};

export default usePasswordHelpText;
