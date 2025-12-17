import * as React from 'react';
import { useState, useEffect, useCallback } from 'react'
import { useFormikContext } from 'formik';

import { FormikFormGroup } from 'components/common';
import type { GeoIpConfigType } from 'components/maps/configurations/types';
import { Button, Input } from 'components/bootstrap';

const ABSGeoIpFormGroup = () => {
  const { values, setFieldValue } = useFormikContext<GeoIpConfigType>();
  const isKeySet = values.key && 'is_set' in values.key;
  const [isCreate] = useState(() => !isKeySet);
  const [showResetPasswordButton, setShowResetPasswordButton] = useState(isKeySet);

  const setAccessKey = useCallback(
    (nextAccessKey) => {
      setFieldValue('key', { set_value: nextAccessKey });
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
        name="container"
        type="text"
        label="Azure Blob Container Name"
        help="Your Azure Blob Container name."
        labelClassName=""
        required
        wrapperClassName=""
      />
      <FormikFormGroup
        name="account_name"
        type="text"
        label="Azure account"
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
          name="key"
          id="key"
          type="password"
          label="Azure account key"
          onChange={({ target: { value } }) => setAccessKey(value)}
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
