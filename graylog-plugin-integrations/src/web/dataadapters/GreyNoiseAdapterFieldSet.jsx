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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';

import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';

const GreyNoiseAdapterFieldSet = ({ config, updateConfig, validationMessage, validationState }) => {
  const isCreate = useRef(!config.api_token?.is_set);
  const [showResetPasswordButton, setShowResetPasswordButton] = useState(config.api_token?.is_set === true);

  const setUserPassword = useCallback((nextUserPassword) => {
    updateConfig({ ...config, api_token: nextUserPassword });
  }, [updateConfig, config]);

  useEffect(() => {
    // Set a default value on `api_token` that the server can deserialize
    if (config.api_token?.is_set !== undefined) {
      // Keeping value is only helpful when editing, but since setting '' as value throws an error during
      // validation, this at least avoids users seeing validation errors constantly.
      setUserPassword({ keep_value: true });
    }
  }, [setUserPassword, config.api_token]);

  const handleUserPasswordChange = ({ target }) => {
    const typedPassword = target.value;
    let nextPassword = { set_value: typedPassword };

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
            <Input id="api_token"
                   label="User Password"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Button onClick={toggleUserPasswordReset}>Reset token</Button>
            </Input>
        ) : (
            <Input type="password"
                   id="api_token"
                   name="api_token"
                   label="API Token"
                   buttonAfter={!isCreate.current ? (
                       <Button type="button" onClick={toggleUserPasswordReset}>
                         Undo Reset
                       </Button>
                   ) : undefined}
                   onChange={handleUserPasswordChange}
                   help={validationMessage('api_token', 'The API Token.')}
                   bsStyle={validationState('api_token')}
                   value={config.api_token?.set_value || ''}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   required />
        )}
      </fieldset>
  );
};

GreyNoiseAdapterFieldSet.propTypes = {
  config: PropTypes.shape({
    api_token: PropTypes.object.isRequired,
  }).isRequired,
  updateConfig: PropTypes.func.isRequired,
  handleFormEvent: PropTypes.func.isRequired,
  validationState: PropTypes.func.isRequired,
  validationMessage: PropTypes.func.isRequired,
};

export default GreyNoiseAdapterFieldSet;
