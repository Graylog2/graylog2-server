// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';

import type { LdapCreate } from 'logic/authentication/ldap/types';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { Button } from 'components/graylog';
import { Spinner } from 'components/common';

import ConnectionErrors, { NotificationContainer } from './ConnectionErrors';
import BackendWizardContext from './contexts/BackendWizardContext';

const _addRequiredRequestPayload = (formValues) => {
  const neccessaryAttributes = { ...formValues };

  if (!neccessaryAttributes.config.user_search_base) {
    neccessaryAttributes.config.user_search_base = '';
  }

  if (!neccessaryAttributes.config.user_search_pattern) {
    neccessaryAttributes.config.user_search_pattern = '';
  }

  return neccessaryAttributes;
};

type Props = {
  prepareSubmitPayload: () => LdapCreate,
};

const ServerConnectionTest = ({ prepareSubmitPayload }: Props) => {
  const { authBackendMeta } = useContext(BackendWizardContext);
  const [{ loading, success, message, errors }, setConnectionStatus] = useState({ loading: false, success: false, message: undefined, errors: undefined });

  const _handleConnectionCheck = () => {
    const payload = _addRequiredRequestPayload(prepareSubmitPayload());

    setConnectionStatus({ loading: true, message: undefined, errors: undefined, success: false });

    AuthenticationDomain.testConnection({ backend_configuration: payload, backend_id: authBackendMeta.backendId }).then((response) => {
      setConnectionStatus({ loading: false, message: response?.message, success: response?.success, errors: response?.errors });
    });
  };

  return (
    <>
      <p>
        Performs a background connection check with the address and credentials defined in the step &quot;Server Configuration&quot;.
      </p>
      <Button onClick={_handleConnectionCheck} type="button">
        {loading ? <Spinner delay={0} /> : 'Start Check'}
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
