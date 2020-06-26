// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import type { AvailableGrantees, AvailableRoles } from 'logic/permissions/EntityShareState';
import Role from 'logic/permissions/Role';
import Grantee from 'logic/permissions/Grantee';
import { Button } from 'components/graylog';
import Select from 'components/common/Select';

const FormElements = styled.div`
  display: flex;
`;

const Errors: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  width: 100%;
  margin-top: 3px;
  color: ${theme.colors.variant.danger};

  > * {
    margin-right: 5px;

    :last-child {
      margin-right: 0;
    }
  }
`);

const GranteesSelect = styled(Select)`
  flex: 1;
`;

const RolesSelect = styled(Select)`
  flex: 0.5;
`;

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const _granteesOptions = (grantees: AvailableGrantees) => (
  grantees.map((grantee) => (
    { label: `${grantee.type} : ${grantee.title}`, value: grantee.id }
  )).toJS()
);

const _rolesOptions = (roles: AvailableRoles) => (
  roles.map((role) => (
    { label: role.title, value: role.id }
  )).toJS()
);

const isRequired = (field) => (value) => (!value ? `The ${field} is required` : undefined);

type FormData = {
  granteeId: $PropertyType<Grantee, 'id'>,
  roleId: $PropertyType<Role, 'id'>,
};

type Props = {
  availableGrantees: AvailableGrantees,
  availableRoles: AvailableRoles,
  onSubmit: (FormData) => void,
};

const GranteesSelector = ({ availableGrantees, availableRoles, onSubmit }: Props) => {
  const granteesOptions = _granteesOptions(availableGrantees);
  const rolesOptions = _rolesOptions(availableRoles);

  return (
    <Formik onSubmit={onSubmit} initialValues={{ granteeId: undefined, roleId: undefined }}>
      {({ isSubmitting, isValid, errors }) => (
        <Form>
          <FormElements>
            <Field name="granteeId" validate={isRequired('grantee')}>
              {({ field: { name, value, onChange } }) => (
                <GranteesSelect placeholder="Search for users and teams"
                                options={granteesOptions}
                                onChange={(granteeId) => onChange({ target: { value: granteeId, name } })}
                                value={value} />
              )}
            </Field>
            <Field name="roleId" validate={isRequired('role')}>
              {({ field: { name, value, onChange } }) => (
                <RolesSelect placeholder="Select a role"
                             options={rolesOptions}
                             onChange={(roleId) => onChange({ target: { value: roleId, name } })}
                             value={value} />
              )}
            </Field>
            <SubmitButton type="submit"
                          bsStyle="success"
                          disabled={isSubmitting || !isValid}>
              Add Collaborator
            </SubmitButton>
          </FormElements>
          {errors
            && (
            <Errors>
              {Object.entries(errors).map(([fieldKey, value]: [string, mixed]) => {
                return <span key={fieldKey}>{String(value)}.</span>;
              })}
            </Errors>
            )}

        </Form>
      )}
    </Formik>
  );
};

export default GranteesSelector;
