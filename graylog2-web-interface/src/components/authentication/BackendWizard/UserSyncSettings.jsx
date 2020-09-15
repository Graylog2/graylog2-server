// @flow strict
import * as React from 'react';
import { useState, useEffect, useContext } from 'react';
import { Formik, Form, Field } from 'formik';

import FormUtils from 'util/FormsUtils';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { FormikFormGroup, Select } from 'components/common';
import { Button, ButtonToolbar, Row, Col, Panel } from 'components/graylog';
import { Input } from 'components/bootstrap';

import BackendWizardContext from './contexts/BackendWizardContext';

type Props = {
  help?: {
    userSearchBase?: React.Node,
    userSearchPattern?: React.Node,
    displayNameAttribute?: React.Node,
    defaultRoles?: React.Node,
  },
  disableSubmitAll: boolean,
  onChange: (SyntheticInputEvent<HTMLInputElement> | { target: { value: string, name: string, checked?: boolean } }) => void,
  onSubmit: (nextStepKey: string) => void,
  onSubmitAll: () => void,
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
  displayNameAttribute: (
    <span>
      Which Active Directory attribute to use for the full name of the user in Graylog, e.g. <code>displayName</code>.<br />
      Try to load a test user using the form below, if you are unsure which attribute to use.
    </span>
  ),
  defaultRoles: (
    'The default Graylog role determines whether a user created via LDAP can access the entire system, or has limited access.'
  ),
};

const UserSyncSettings = ({ help: propsHelp, onSubmit, onSubmitAll, disableSubmitAll, onChange }: Props) => {
  const help = { ...defaultHelp, ...propsHelp };
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);
  const [rolesOptions, setRolesOptions] = useState([]);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited).then((roles) => {
      if (roles) {
        const options = roles.list.map((role) => ({ label: role.name, value: role.name })).toArray();
        setRolesOptions(options);
      }
    });
  }, []);

  return (
    <Formik initialValues={stepsState.formValues} onSubmit={() => onSubmit('groupSync')} validateOnMount>
      {({ isSubmitting, isValid, values }) => (
        <Form onChange={(event) => onChange(event)} className="form form-horizontal">
          <FormikFormGroup label="Search Base DN"
                           name="userSearchBase"
                           placeholder="System User DN"
                           required
                           validate={FormUtils.validation.isRequired('System Username')}
                           help={help.userSearchBase} />

          <FormikFormGroup label="Search Pattern"
                           name="userSearchPattern"
                           placeholder="Search Pattern"
                           required
                           validate={FormUtils.validation.isRequired('Search Pattern')}
                           help={help.userSearchPattern} />

          <FormikFormGroup label="Display Name Attirbute"
                           name="displayNameAttribute"
                           placeholder="Display Name Attirbute"
                           validate={FormUtils.validation.isRequired('Display Name Attribute')}
                           required
                           help={help.displayNameAttribute} />

          <Row>
            <Col sm={9} smOffset={3}>
              <Panel bsStyle="info">
                Changing the static role assignment will only affect to new users created via LDAP/Active Directory!<br />
                Existing user accounts will be updated on their next login, or if you edit their roles manually.
              </Panel>
            </Col>
          </Row>

          <Field name="defaultRoles" validate={FormUtils.validation.isRequired('Default Roles')}>
            {({ field: { name, value, onChange: onFieldChange } }) => {
              return (
                <Input id="default-roles-select"
                       label="Default Roles"
                       help={help.defaultRoles}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9">
                  <Select inputProps={{ 'aria-label': 'Search for roles' }}
                          onChange={(roleName) => {
                            onFieldChange({ target: { value: roleName, name } });
                            onChange({ target: { value: roleName, name } });
                          }}
                          options={rolesOptions}
                          placeholder="Search for roles"
                          multi
                          value={value} />
                </Input>
              );
            }}
          </Field>

          <ButtonToolbar className="pull-right">
            <Button type="button"
                    onClick={onSubmitAll}
                    disabled={!isValid || isSubmitting || disableSubmitAll}>
              Finish & Save Identity Provider
            </Button>
            <Button bsStyle="primary"
                    type="submit"
                    disabled={!isValid || isSubmitting}>
              Next: Group Synchronisation
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

UserSyncSettings.defaultProps = {
  help: {},
};

export default UserSyncSettings;
