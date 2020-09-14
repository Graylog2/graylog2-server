// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';

import { Button, Alert } from 'components/graylog';
import { Spinner } from 'components/common';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';

import BackendWizardContext from './contexts/BackendWizardContext';

const ServerConnectionTest = () => {
  const [{ loading, success, message, errors }, setConnectionStatus] = useState({ loading: false, success: false, message: undefined, errors: undefined });
  const { formValues, prepareSubmitPayload } = useContext(BackendWizardContext);

  const _handleConnectionCheck = () => {
    const payload = prepareSubmitPayload(formValues);

    setConnectionStatus({ loading: true, message: undefined, errors: undefined, success: false });

    AuthenticationDomain.testConnection({ backend_configuration: payload }).then((response) => {
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
      <Button type="button" onClick={() => _handleConnectionCheck()}>
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
