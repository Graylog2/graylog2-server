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
import { useState, useEffect, useCallback } from 'react';
import { useFormikContext, Field } from 'formik';
import styled from 'styled-components';

import { FormikFormGroup, Select, Icon } from 'components/common';
import type { GeoIpConfigType, AzureAuthType, EncryptedValue } from 'components/maps/configurations/types';
import { Button, Input, Table } from 'components/bootstrap';

const AZURE_AUTH_TYPE_OPTIONS = [
  { value: 'automatic', label: 'Automatic' },
  { value: 'keysecret', label: 'Key & Secret' },
];

const SectionTitle = styled.p`
  font-weight: bold;
  font-size: 1.2em;
  margin: 0 0 12px;
`;

const SectionNote = styled.p`
  font-style: italic;
  margin: 3px 0 0;
`;

const StyledTable = styled(Table)`
  margin: 0;
`;

const EnvVarList = styled.div`
  margin-top: 4px;
`;

const ABSGeoIpFormGroup = () => {
  const { values, setFieldValue } = useFormikContext<GeoIpConfigType>();
  const isKeySet = values.azure_account_key && 'is_set' in values.azure_account_key;
  const [isCreate] = useState(() => !isKeySet);
  const [showResetPasswordButton, setShowResetPasswordButton] = useState(isKeySet);
  const [authType, setAuthType] = useState<AzureAuthType>(values.azure_auth_type ?? 'automatic');

  const setAccessKey = useCallback(
    (nextAccessKey: EncryptedValue | undefined) => {
      setFieldValue('azure_account_key', nextAccessKey);
    },
    [setFieldValue],
  );

  useEffect(() => {
    if (isKeySet && authType === 'keysecret') {
      setAccessKey({ keep_value: true });
    }
  }, [isKeySet, setAccessKey, authType]);

  const toggleAccountKeyReset = useCallback(() => {
    if (showResetPasswordButton) {
      setAccessKey({ delete_value: true });
      setShowResetPasswordButton(false);

      return;
    }

    setAccessKey({ keep_value: true });
    setShowResetPasswordButton(true);
  }, [setAccessKey, showResetPasswordButton]);

  const handleAuthTypeChange = useCallback(
    (option: string) => {
      const newAuthType = option as AzureAuthType;
      setAuthType(newAuthType);
      setFieldValue('azure_auth_type', newAuthType);

      if (newAuthType === 'automatic') {
        // Clear key/secret fields when switching to automatic
        setFieldValue('azure_account', undefined);
        setFieldValue('azure_account_key', undefined);
      }
    },
    [setFieldValue],
  );

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
      <Field>
        {() => (
          <Input id="azure-auth-type-select" label="Azure Authentication Type">
            <Select
              id="azureAuthenticationType"
              inputId="azure-auth-type-input"
              name="azure_auth_type"
              placeholder="Select Authentication Type"
              options={AZURE_AUTH_TYPE_OPTIONS}
              onChange={handleAuthTypeChange}
              value={authType}
            />
          </Input>
        )}
      </Field>
      {authType === 'automatic' && (
        <StyledTable condensed>
          <thead>
            <tr>
              <td colSpan={2}>
                <SectionTitle>Automatic authentication will attempt each of the following in the listed order.</SectionTitle>
              </td>
            </tr>
          </thead>

          <tbody>
            <tr>
              <th>Environment</th>
              <td>
                Authenticates via environment variables.
                <EnvVarList>
                  The required environment variables for service principal authentication using client secret are as follows:{' '}
                  <code>AZURE_CLIENT_ID</code>, <code>AZURE_CLIENT_SECRET</code>, <code>AZURE_TENANT_ID</code>
                </EnvVarList>
                <EnvVarList>
                  The required environment variables for service principal authentication using client certificate are as
                  follows: <code>AZURE_CLIENT_ID</code>, <code>AZURE_CLIENT_CERTIFICATE_PATH</code>,{' '}
                  <code>AZURE_CLIENT_CERTIFICATE_PASSWORD</code>, <code>AZURE_TENANT_ID</code>
                </EnvVarList>
              </td>
            </tr>
            <tr>
              <th>Workload Identity</th>
              <td>For use on an Azure host with Workload Identity enabled.</td>
            </tr>
            <tr>
              <th>Managed Identity</th>
              <td>For use on an Azure host with Managed Identity enabled.</td>
            </tr>
            <tr>
              <th>Azure CLI</th>
              <td>Uses current Azure CLI account.</td>
            </tr>
            <tr>
              <th>Broker</th>
              <td>
                Use the default account logged into the OS via a broker. Requires that the{' '}
                <code>azure-identity-broker</code> package is installed.
              </td>
            </tr>
          </tbody>

          <tfoot>
            <tr>
              <td colSpan={2}>
                <SectionNote>
                  For more information, check out the{' '}
                  <a
                    href="https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#defaultazurecredential"
                    target="_blank"
                    rel="noopener noreferrer">
                    Azure DefaultAzureCredential Documentation <Icon name="open_in_new" />
                  </a>
                </SectionNote>
              </td>
            </tr>
          </tfoot>
        </StyledTable>
      )}
      {authType === 'keysecret' && (
        <>
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
      )}
    </>
  );
};

export default ABSGeoIpFormGroup;
