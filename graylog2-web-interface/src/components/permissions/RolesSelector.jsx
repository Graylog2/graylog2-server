// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { type PaginatedListType } from 'stores/roles/AuthzRolesStore';
import Role from 'logic/roles/Role';
import { Button } from 'components/graylog';
import { Select } from 'components/common';
import User from 'logic/users/User';

type Props = {
  onSubmit: (role: Role) => Promise<void>,
  user: User,
};

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const FormElements = styled.div`
  display: flex;
`;

const RoleSelectOption = styled.div`
  display: flex;
  align-items: center;
`;

const StyledSelect = styled(Select)`
  flex: 1;
`;

const _renderRoleOption = ({ label }: { label: string }) => (
  <RoleSelectOption>{label}</RoleSelectOption>
);

const _options = (roles: Immutable.List<Role>, user: User) => roles
  .filter((r) => !user.roles.includes(r.name))
  .toArray()
  .map((r) => ({ label: r.name, value: r.name, role: r }));

const _assignRole = (username, selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting) => {
  const selectedRole = roles.find((r) => r.name === selectedRoleName);

  if (!selectedRole) {
    throw Error(`Role assigment for user ${username} failed, because role with name ${selectedRoleName ?? '(undefined)'} does not exist`);
  }

  setIsSubmitting(true);

  onSubmit(selectedRole).then(() => {
    setSelectedRoleName();
    setIsSubmitting(false);
  });
};

const RolesSelector = ({ user, onSubmit }: Props) => {
  const [roles, setRoles] = useState(Immutable.List());
  const [selectedRoleName, setSelectedRoleName] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const _onSubmit = () => _assignRole(user.username, selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting);
  const options = _options(roles, user);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited)
      .then((paginatedRoles: ?PaginatedListType) => paginatedRoles && setRoles(paginatedRoles.list));
  }, [user]);

  return (
    <div>
      <FormElements>
        <StyledSelect inputProps={{ 'arial-label': 'Search for roles' }}
                      onChange={setSelectedRoleName}
                      optionRenderer={_renderRoleOption}
                      options={options}
                      placeholder="Search for roles"
                      value={selectedRoleName} />
        <SubmitButton bsStyle="success"
                      onClick={_onSubmit}
                      disabled={isSubmitting || !selectedRoleName}
                      title="Assign Role"
                      type="submit">
          Assign Role
        </SubmitButton>
      </FormElements>
    </div>
  );
};

export default RolesSelector;
