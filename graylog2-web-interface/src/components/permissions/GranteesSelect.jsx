// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';

import type { AvailableGrantees, AvailableRoles } from 'logic/permissions/EntityShareState';
import Role from 'logic/permissions/Role';
import Grantee from 'logic/permissions/Grantee';
import { Button } from 'components/graylog';
import Select from 'components/common/Select';

const Container = styled.div`
  display: flex;
`;

const StyledGranteesSelect = styled(Select)`
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

type FormData = {
  granteeId: $PropertyType<Grantee, 'id'>,
  roleId: $PropertyType<Role, 'id'>,
};

type Props = {
  availableGrantees: AvailableGrantees,
  availableRoles: AvailableRoles,
  onSubmit: (FormData) => void,
};

const GranteesSelect = ({ availableGrantees, availableRoles, onSubmit }: Props) => {
  const _onSubmit = useCallback(({ granteeId, roleId }) => onSubmit({ granteeId, roleId }), [onSubmit]);
  const granteesOptions = _granteesOptions(availableGrantees);
  const rolesOptions = _rolesOptions(availableRoles);

  return (
    <Formik onSubmit={_onSubmit} initialValues={{ granteeId: undefined, roleId: undefined }}>
      {({ isSubmitting, isValid }) => (
        <Form>
          <Container>
            <Field name="granteeId">
              {({ field: { name, value, onChange } }) => (
                <StyledGranteesSelect placeholder="Search for users and teams"
                                      options={granteesOptions}
                                      onChange={(granteeId) => onChange({ target: { value: granteeId, name } })}
                                      value={value} />
              )}
            </Field>
            <Field name="roleId">
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
          </Container>
        </Form>
      )}
    </Formik>
  );
};

export default GranteesSelect;
