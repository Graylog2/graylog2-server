// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';
import URI from 'urijs';

import { Button, Alert } from 'components/graylog';
import { Spinner } from 'components/common';
import ActionsProvider from 'injection/ActionsProvider';

import ServiceStepsContext from '../contexts/ServiceStepsContext';

const LdapActions = ActionsProvider.getActions('Ldap');

const ServerConnectionCheck = () => {
  const [{ loading, success, error }, setConnectionStatus] = useState({ loading: false, success: false, error: undefined });
  const { formValues: { 'server-configuration': serverConfig } } = useContext(ServiceStepsContext);

  const _handleConnectionCheck = () => {
    const {
      uriHost,
      uriPort,
      systemUsername,
      systemPassword,
      useStartTLS,
      trustAllCertificates,
    } = serverConfig ?? {};
    const ldapURI = `${new URI('').host(uriHost).port(uriPort).scheme('ldap')}`;

    setConnectionStatus({ loading: true, error: undefined, success: false });

    LdapActions.testServerConnection.triggerPromise({
      ldap_uri: ldapURI,
      system_username: systemUsername,
      system_password: systemPassword,
      use_start_tls: useStartTLS,
      trust_all_certificates: trustAllCertificates,
    }).then((result) => {
      if (result.connected) {
        setConnectionStatus({ loading: false, success: true, error: undefined });
      } else {
        setConnectionStatus({ loading: false, success: undefined, error: result.exception });
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
      {success && <Alert bsStyle="success">Connection to server was successful</Alert>}
      {error && <Alert bsStyle="danger">{error}</Alert>}
    </>
  );
};

export default ServerConnectionCheck;
