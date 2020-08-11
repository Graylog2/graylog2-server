// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import Role from 'logic/roles/Role';
import { AuthzRolesActions, type PaginatedListType } from 'stores/roles/AuthzRolesStore';
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

const RolesSelector = ({ user, onSubmit }: Props) => {
  const [roles, setRoles] = useState(Immutable.List());
  const [selectedRoleName, setSelectedRoleName] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const options = roles
    .filter((r) => !user.roles.includes(r.name))
    .toArray()
    .map((r) => ({ label: r.name, value: r.name, role: r }));

  const _assignRole = () => {
    const selectedRole = roles.find((r) => r.name === selectedRoleName);

    if (!selectedRole) {
      throw Error(`Role assigment for user ${user.username} failed, because role with name ${selectedRoleName ?? '(undefined)'} does not exist`);
    }

    setIsSubmitting(true);

    onSubmit(selectedRole).then(() => {
      setSelectedRoleName();
      setIsSubmitting(false);
    });
  };

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesActions.loadPaginated(...getUnlimited)
      .then((response: PaginatedListType) => {
        const { list } = response;
        setRoles(list);
      });
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
                      onClick={_assignRole}
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
