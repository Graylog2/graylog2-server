// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';

import type { LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { Button, Alert } from 'components/graylog';
import { Spinner } from 'components/common';

import BackendWizardContext from './contexts/BackendWizardContext';

type Props = {
  prepareSubmitPayload: () => LdapCreate,
};

const ServerConnectionTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const [{ loading, success, message, errors }, setConnectionStatus] = useState({ loading: false, success: false, message: undefined, errors: undefined });

  const _handleConnectionCheck = () => {
    const payload = prepareSubmitPayload();

    setConnectionStatus({ loading: true, message: undefined, errors: undefined, success: false });

    AuthenticationDomain.testConnection({ backend_configuration: payload, backend_id: authBackendMeta.backendId }).then((response) => {
      if (response?.success) {
        setConnectionStatus({ loading: false, message: response.message, success: true, errors: undefined });
      } else {
        setConnectionStatus({ loading: false, message: undefined, success: undefined, errors: response?.errors });
      }
    });
  };

  return (
    <>
      <p>
        Performs a background connection check with the address and credentials above.
      </p>
      <Button onClick={_handleConnectionCheck} type="button">
        {loading ? <Spinner delay={0} /> : 'Start Check'}
      </Button>
      {success && <Alert bsStyle="success">{message}</Alert>}
      {errors && (
        <Alert bsStyle="danger">
          {message}
          <ul>
            {errors.map((error) => {
              return <li key={error}>{error}</li>;
            })}
          </ul>
        </Alert>
      )}
    </>
  );
};

export default ServerConnectionTest;
