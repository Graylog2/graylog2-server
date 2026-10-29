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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button, Input } from 'components/bootstrap';
import type { ValidationState } from 'components/common/types';
import type { EncryptedFieldValue } from 'components/configurationforms/types';

type GreyNoiseAdapterFieldSetProps = {
  config: {
    api_token: any;
  };
  setFieldValue: (...args: any[]) => void;
  validationState: (...args: any[]) => ValidationState;
  validationMessage: (...args: any[]) => string;
};

const GreyNoiseAdapterFieldSet = ({
  config,
  setFieldValue,
  validationMessage,
  validationState,
}: GreyNoiseAdapterFieldSetProps) => {
  const { t } = useTranslation('integrations');
  const [isCreate] = useState(() => !config.api_token?.keep_value);
  const [showResetPasswordButton, setShowResetPasswordButton] = useState(!!config.api_token?.keep_value);

  const setUserPassword = (nextUserPassword: EncryptedFieldValue<string>) => {
    setFieldValue('config.api_token', nextUserPassword);
  };

  const handleUserPasswordChange = ({ target }) => {
    const typedPassword = target.value;
    let nextPassword: {} = { set_value: typedPassword };

    if (typedPassword === '') {
      nextPassword = { delete_value: true };
    }

    setUserPassword(nextPassword);
  };

  const toggleUserPasswordReset = () => {
    if (showResetPasswordButton) {
      setUserPassword({ delete_value: true });
      setShowResetPasswordButton(false);

      return;
    }

    setUserPassword({ keep_value: true });
    setShowResetPasswordButton(true);
  };

  return (
    <fieldset>
      {showResetPasswordButton ? (
        <Input
          id="api_token"
          label={t('grey_noise_adapter_field_set.user_password_label')}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9">
          <Button onClick={toggleUserPasswordReset}>{t('grey_noise_adapter_field_set.reset_token_button')}</Button>
        </Input>
      ) : (
        <Input
          type="password"
          id="api_token"
          name="api_token"
          label={t('grey_noise_adapter_field_set.api_token_label')}
          buttonAfter={
            !isCreate ? (
              <Button type="button" onClick={toggleUserPasswordReset}>
                {t('grey_noise_adapter_field_set.undo_reset_button')}
              </Button>
            ) : undefined
          }
          onChange={handleUserPasswordChange}
          help={validationMessage('api_token', t('grey_noise_adapter_field_set.api_token_help'))}
          bsStyle={validationState('api_token')}
          value={config.api_token?.set_value || ''}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
          required
        />
      )}
    </fieldset>
  );
};

export default GreyNoiseAdapterFieldSet;
