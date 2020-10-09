// @flow strict
import * as React from 'react';
import { useState, useEffect, useContext } from 'react';
import { Formik, Form, Field } from 'formik';

import { validateField, formHasErrors } from 'util/FormsUtils';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { Alert, Button, ButtonToolbar, Row, Col, Panel } from 'components/graylog';
import { Icon, FormikFormGroup, Select } from 'components/common';
import { Input } from 'components/bootstrap';

import BackendWizardContext from './contexts/BackendWizardContext';

export type StepKeyType = 'user-synchronization';
export const STEP_KEY: StepKeyType = 'user-synchronization';
export const FORM_VALIDATION = {
  defaultRoles: { required: true },
  userFullNameAttribute: { required: true },
  userNameAttribute: { required: true },
  userSearchBase: { required: true },
  userSearchPattern: { required: true },
};

type Props = {
  formRef: React.Ref<typeof Formik>,
  help?: {
    defaultRoles?: React.Node,
    userFullNameAttribute?: React.Node,
    userNameAttribute?: React.Node,
    userSearchBase?: React.Node,
    userSearchPattern?: React.Node,
  },
  onSubmit: () => void,
  onSubmitAll: () => void,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const defaultHelp = {
  userSearchBase: (
    <span>
      The base tree to limit the Active Directory search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
    </span>
  ),
  userSearchPattern: (
    <span>
      For example <code className="text-nowrap">{'(&(objectClass=user)(sAMAccountName={0}))'}</code>.{' '}
      The string <code>{'{0}'}</code> will be replaced by the entered username.
    </span>
  ),
  userNameAttribute: (
    <span>
      Which Active Directory attribute to use for the username of the user in Graylog.<br />
      Try to load a test user using the sidebar form, if you are unsure which attribute to use.
    </span>
  ),
  userFullNameAttribute: (
    <span>
      Which Active Directory attribute to use for the full name of the user in Graylog, e.g. <code>displayName</code>.<br />
    </span>
  ),
  defaultRoles: (
    'The default Graylog roles determine whether a user created via LDAP can access the entire system, or has limited access.'
  ),
};

const UserSyncStep = ({ help: propsHelp, formRef, onSubmit, onSubmitAll, submitAllError, validateOnMount }: Props) => {
  const help = { ...defaultHelp, ...propsHelp };
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const [rolesOptions, setRolesOptions] = useState([]);

  const _onSubmitAll = (validateForm) => {
    validateForm().then((errors) => {
      if (!formHasErrors(errors)) {
        onSubmitAll();
      }
    });
  };

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited).then((roles) => {
      if (roles) {
        const options = roles.list.map((role) => ({ label: role.name, value: role.id })).toArray();
        setRolesOptions(options);
      }
    });
  }, []);

  return (
    // $FlowFixMe innerRef works as expected
    <Formik initialValues={stepsState.formValues}
            innerRef={formRef}
            onSubmit={onSubmit}
            validateOnBlur={false}
            validateOnChange={false}
            validateOnMount={validateOnMount}>
      {({ isSubmitting, validateForm }) => (
        <Form className="form form-horizontal">
          <FormikFormGroup help={help.userSearchBase}
                           label="Search Base DN"
                           name="userSearchBase"
                           placeholder="Search Base DN"
                           validate={validateField(FORM_VALIDATION.userSearchBase)} />

          <FormikFormGroup help={help.userSearchPattern}
                           label="Search Pattern"
                           name="userSearchPattern"
                           placeholder="Search Pattern"
                           validate={validateField(FORM_VALIDATION.userSearchPattern)} />

          <FormikFormGroup help={help.userNameAttribute}
                           label="Name Attribute"
                           name="userNameAttribute"
                           placeholder="Name Attribute"
                           validate={validateField(FORM_VALIDATION.userNameAttribute)} />

          <FormikFormGroup help={help.userFullNameAttribute}
                           label="Full Name Attribute"
                           name="userFullNameAttribute"
                           placeholder="Full Name Attribute"
                           validate={validateField(FORM_VALIDATION.userFullNameAttribute)} />

          <Row>
            <Col sm={9} smOffset={3}>
              <Panel bsStyle="info">
                Changing the static role assignment will only affect to new users created via LDAP/Active Directory!<br />
                Existing user accounts will be updated on their next login, or if you edit their roles manually.
              </Panel>
            </Col>
          </Row>

          <Field name="defaultRoles" validate={validateField(FORM_VALIDATION.defaultRoles)}>
            {({ field: { name, value, onChange, onBlur }, meta: { error } }) => (
              <Input bsStyle={error ? 'error' : undefined}
                     help={help.defaultRoles}
                     error={error}
                     id="default-roles-select"
                     label="Default Roles"
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select inputProps={{ 'aria-label': 'Search for roles' }}
                        multi
                        onBlur={onBlur}
                        onChange={(selectedRoles) => onChange({ target: { value: selectedRoles, name } })}
                        options={rolesOptions}
                        placeholder="Search for roles"
                        value={value} />
              </Input>
            )}
          </Field>

          <Row>
            <Col sm={9} smOffset={3}>
              <Alert bsStyle="info">
                <Icon name="info-circle" />{' '}
                We recommend you test your user login in the sidebar panel to verify your settings.
              </Alert>
            </Col>
          </Row>

          {submitAllError}

          <ButtonToolbar className="pull-right">
            <Button disabled={isSubmitting}
                    onClick={() => _onSubmitAll(validateForm)}
                    type="button">
              Finish & Save Identity Service
            </Button>
            <Button bsStyle="primary"
                    disabled={isSubmitting}
                    type="submit">
              Next: Group Synchronisation
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

UserSyncStep.defaultProps = {
  help: {},
};

export default UserSyncStep;
