// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { Button } from 'components/graylog';
import { Spinner } from 'components/common';

import ConnectionErrors, { NotificationContainer } from './ConnectionErrors';
import BackendWizardContext from './BackendWizardContext';

const _addRequiredRequestPayload = (formValues) => {
  const necessaryAttributes = { ...formValues };

  if (!necessaryAttributes.config.user_search_base) {
    necessaryAttributes.config.user_search_base = '';
  }

  if (!necessaryAttributes.config.user_search_pattern) {
    necessaryAttributes.config.user_search_pattern = '';
  }

  return necessaryAttributes;
};

type Props = {
  prepareSubmitPayload: () => WizardSubmitPayload,
};

const ServerConnectionTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const [{ loading, success, message, errors }, setConnectionStatus] = useState({ loading: false, success: false, message: undefined, errors: undefined });

  const _handleConnectionCheck = () => {
    const payload = _addRequiredRequestPayload(prepareSubmitPayload());

    setConnectionStatus({ loading: true, message: undefined, errors: undefined, success: false });

    AuthenticationDomain.testConnection({ backend_configuration: payload, backend_id: authBackendMeta.backendId }).then((response) => {
      setConnectionStatus({ loading: false, message: response?.message, success: response?.success, errors: response?.errors });
    }).catch((error) => {
      const requestErrors = [error?.message, error?.additional?.res?.text];
      setConnectionStatus({ loading: false, message: undefined, errors: requestErrors, success: false });
    });
  };

  return (
    <>
      <p>
        Performs a background connection check with the address and credentials defined in the step &quot;Server Configuration&quot;.
      </p>
      <Button onClick={_handleConnectionCheck} type="button">
        {loading ? <Spinner delay={0} text="Test Server Connection" /> : 'Test Server Connection'}
      </Button>
      {success && (
        <NotificationContainer bsStyle="success">
          <b>{message}</b>
        </NotificationContainer>
      )}
      {(errors && errors.length >= 1) && (
        <ConnectionErrors errors={errors} message={message} />
      )}
    </>
  );
};

export default ServerConnectionTest;
