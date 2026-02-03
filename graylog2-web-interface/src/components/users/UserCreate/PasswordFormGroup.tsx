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

import { FormikInput } from 'components/common';
import { Row, Col, Input } from 'components/bootstrap';
import { DEFAULT_PASSWORD_COMPLEXITY_CONFIG, passwordComplexityErrors } from 'logic/users/passwordComplexity';
import type { PasswordComplexityConfigType } from 'stores/configurations/ConfigurationsStore';
import PasswordRules from 'components/users/PasswordRules';
import usePasswordHelpText from 'components/users/usePasswordHelpText';

export const validatePasswords = (
  errors: { [name: string]: React.ReactNode },
  password: string,
  passwordRepeat: string,
  passwordConfig: PasswordComplexityConfigType = DEFAULT_PASSWORD_COMPLEXITY_CONFIG,
) => {
  const newErrors = { ...errors };
  const complexityErrors = passwordComplexityErrors(password, passwordConfig);

  if (complexityErrors.length > 0) {
    newErrors.password = <PasswordRules lines={complexityErrors} />;
  }

  if (password && passwordRepeat) {
    const passwordMatches = password === passwordRepeat;

    if (!passwordMatches) {
      newErrors.password_repeat = 'Passwords do not match';
    }
  }

  return newErrors;
};

type Props = {
  passwordComplexityConfig: PasswordComplexityConfigType;
};

const PasswordFormGroup = ({ passwordComplexityConfig }: Props) => {
  const minLength = passwordComplexityConfig.min_length;
  const effectiveHelpText = usePasswordHelpText({ passwordComplexityConfig });

  return (
    <Input
      id="password-field"
      label="Password"
      help={effectiveHelpText}
      labelClassName="col-sm-3"
      wrapperClassName="col-sm-9">
      <Row className="no-bm">
        <Col sm={6}>
          <FormikInput
            name="password"
            id="password"
            maxLength={100}
            type="password"
            placeholder="Password"
            required
            formGroupClassName="form-group no-bm"
            wrapperClassName="col-xs-12"
            minLength={minLength}
          />
        </Col>
        <Col sm={6}>
          <FormikInput
            name="password_repeat"
            id="password_repeat"
            maxLength={100}
            type="password"
            placeholder="Repeat password"
            formGroupClassName="form-group no-bm"
            required
            wrapperClassName="col-xs-12"
            minLength={minLength}
          />
        </Col>
      </Row>
    </Input>
  );
};

export default PasswordFormGroup;
