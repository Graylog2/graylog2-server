// @flow strict
import * as React from 'react';
import { useState, useContext } from 'react';
import URI from 'urijs';
import { Formik, Form } from 'formik';
import styled, { css } from 'styled-components';

import { FormikField, Spinner, Icon } from 'components/common';
import { Button, Row, Col, Panel } from 'components/graylog';
import ActionsProvider from 'injection/ActionsProvider';
import ObjectUtils from 'util/ObjectUtils';

import ServiceStepsContext from '../contexts/ServiceStepsContext';

const LdapActions = ActionsProvider.getActions('Ldap');

const LoginResultPanel = styled(Panel)`
  h4 {
    margin-bottom: 10px;
  }

  .login-status {
    padding: 0;
    margin-bottom: 10px;
  }

  .login-status li {
    display: inline-block;
    margin-right: 20px;
  }
`;

const StatusIcon = styled(Icon)(({ status, theme }) => `
  color: ${theme.colors.variant[status]};
`);

const _formatLoginStatus = ({ password }, loading, success, error, result) => {
  // Temporary until we defined the correct request payload

  // Don't show any status if login didn't complete
  if (!error && !success) {
    return null;
  }

  const title = `Connection ${error ? 'failed' : 'successful'}`;
  const style = error ? 'danger' : 'success';

  let userFound;

  if (ObjectUtils.isEmpty(result.entry)) {
    userFound = <StatusIcon status="danger" name="times" />;
  } else {
    userFound = <StatusIcon status="success" name="check" />;
  }

  let loginCheck;

  if (result.login_authenticated) {
    loginCheck = <StatusIcon status="success" name="check" />;
  } else if (password === '') {
    loginCheck = <StatusIcon status="info" name="question" />;
  } else {
    loginCheck = <StatusIcon status="danger" name="times" />;
  }

  let serverResponse;

  if (result.exception) {
    serverResponse = <pre>{result.exception}</pre>;
  }

  const attributes = Object.keys(result.entry).map((key) => {
    return [
      <dt key={`${key}-dt`}>{key}</dt>,
      <dd key={`${key}-dd`}>{result.entry[key]}</dd>,
    ];
  });
  const formattedEntry = (attributes.length > 0 ? <dl>{attributes}</dl>
    : <p>LDAP server did not return any attributes for the user.</p>);

  const groups = (result.groups ? result.groups.map((group) => <li key={group}>{group}</li>) : []);
  const formattedGroups = (groups.length > 0 ? <ul style={{ padding: 0 }}>{groups}</ul>
    : <p>LDAP server did not return any groups for the user.</p>);

  return (
    <Row>
      <Col xs={12}>
        <LoginResultPanel header={title} bsStyle={style}>
          <ul className="login-status">
            <li><h4>User found {userFound}</h4></li>
            <li><h4>Login attempt {loginCheck}</h4></li>
          </ul>
          {serverResponse && <h4>Server response</h4>}
          {serverResponse}
          <h4>User&apos;s LDAP attributes</h4>
          {formattedEntry}
          <h4>User&apos;s LDAP groups</h4>
          {formattedGroups}
        </LoginResultPanel>
      </Col>
    </Row>
  );
};

const UserLoginCheck = () => {
  const [{ loading, success, error, result }, setLoginStatus] = useState({ loading: false, success: false, error: undefined, result: undefined });
  const { formValues: { 'server-configuration': serverConfig, 'user-mapping': userMapping } } = useContext(ServiceStepsContext);

  const _handleLoginCheck = ({ username, password }) => {
    setLoginStatus({ loading: true, error: undefined, success: false, result: undefined });
    const ldapURI = `${new URI('').host(serverConfig.uriHost).port(serverConfig.uriPort).scheme('ldap')}`;

    const ldapSettings = {
      active_directory: false,
      additional_default_groups: [],
      default_group: 'Reader',
      display_name_attribute: userMapping.displayNameAttribute,
      enabled: true,
      group_id_attribute: '',
      group_mapping: {},
      group_search_base: '',
      group_search_pattern: '',
      ldap_uri: ldapURI,
      search_base: serverConfig.searchBase,
      search_pattern: serverConfig.searchPattern,
      system_password_set: !!serverConfig.systemPassword,
      system_username: serverConfig.systemUsername,
      trust_all_certificates: serverConfig.trustAllCertificates,
      use_start_tls: serverConfig.useStartTLS,

    };

    LdapActions.testLogin.triggerPromise(ldapSettings, username, password)
      .then(
        (newResult) => {
          if (newResult.connected && (newResult.login_authenticated || !ObjectUtils.isEmpty(newResult.entry))) {
            setLoginStatus({ loading: false, success: true, result: newResult });
          } else {
            setLoginStatus({ loading: false, error: true, result: newResult });
          }
        },
        () => {
          setLoginStatus({
            loading: false,
            error: true,
            result: {
              exception: 'Unable to test login, please try again.',
            },
          });
        },
      );
  };

  return (
    <>
      <p>
        Verify the settings by loading the entry for the given user name. If you omit the password, no authentication attempt will be made.
      </p>
      <Formik onSubmit={_handleLoginCheck} initialValues={{ password: '', username: '' }}>
        {({ isSubmitting, isValid, values }) => (
          <Form className="form">
            <Row>
              <Col sm="6">
                <FormikField label="Username"
                             name="username" />
              </Col>
              <Col sm="6">
                <FormikField label="Password"
                             name="password" />
              </Col>
            </Row>
            <Button type="submit" disabled={isSubmitting || !isValid}>
              {loading ? <Spinner delay={0} /> : 'Test Login'}
            </Button>
            {_formatLoginStatus(values, loading, success, error, result)}
          </Form>
        )}
      </Formik>
    </>
  );
};

export default UserLoginCheck;
