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
import { useState, useEffect, useCallback } from 'react'
import { useFormikContext } from 'formik';

import { FormikFormGroup } from 'components/common';
import type { GeoIpConfigType } from 'components/maps/configurations/types';
import { Button, Input } from 'components/bootstrap';

const ABSGeoIpFormGroup = () => {
  const { values, setFieldValue } = useFormikContext<GeoIpConfigType>();
  const isKeySet = values.azure_account_key && 'is_set' in values.azure_account_key;
  const [isCreate] = useState(() => !isKeySet);
  const [showResetPasswordButton, setShowResetPasswordButton] = useState(isKeySet);

  const setAccessKey = useCallback(
    (nextAccessKey) => {
      setFieldValue('azure_account_key', nextAccessKey);
    },
    [setFieldValue],
  );

  useEffect(() => {
    if (isKeySet) {
      setAccessKey({ keep_value: true });
    }
  }, [isKeySet, setAccessKey]);

  const toggleAccountKeyReset = useCallback(() => {
    if (showResetPasswordButton) {
      setAccessKey({ delete_value: true });
      setShowResetPasswordButton(false);

      return;
    }

    setAccessKey({ keep_value: true });
    setShowResetPasswordButton(true);
  }, [setAccessKey, showResetPasswordButton]);

  return (
    <>
      <FormikFormGroup
        name="azure_container"
        type="text"
        label="Azure Blob Container Name"
        help="Your Azure Blob Container name."
        labelClassName=""
        required
        wrapperClassName=""
      />
      <FormikFormGroup
        name="azure_endpoint"
        type="text"
        label="Azure Blob Endpoint URL"
        help="Your Azure Blob Endpoint URL, only required if you want to override the default endpoint."
        labelClassName=""
        wrapperClassName=""
      />
      <FormikFormGroup
        name="azure_account"
        type="text"
        label="Azure account name"
        placeholder="your-account-name"
        help="The name of your Azure storage account."
        required
        labelClassName=""
        wrapperClassName=""
      />
      {showResetPasswordButton ? (
        <Input id="azure_account_reset" label="Azure Account Key" labelClassName="col-sm-3" wrapperClassName="col-sm-9">
          <Button onClick={toggleAccountKeyReset}>Reset password</Button>
        </Input>
      ) : (
        <Input
          name="azure_account_key"
          id="azure_account_key"
          data-testid="azure-account-key-input"
          type="password"
          label="Azure account key"
          onChange={({ target: { value } }) => setAccessKey({ set_value: value })}
          buttonAfter={
            !isCreate ? (
              <Button type="button" onClick={toggleAccountKeyReset}>
                Undo Reset
              </Button>
            ) : undefined
          }
          placeholder="****************"
          help="The account key for your Azure storage account."
          required
        />
      )}
    </>
  )
}

export default ABSGeoIpFormGroup
