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

type Props = {
  onSubmit: (role: Role) => Promise<void>,
  assignedRolesNames: Immutable.List<string>,
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

const _options = (roles: Immutable.List<Role>, assignedRolesNames) => roles
  .filter((r) => !assignedRolesNames.includes(r.name))
  .toArray()
  .map((r) => ({ label: r.name, value: r.name, role: r }));

const _assignRole = (selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting) => {
  const selectedRole = roles.find((r) => r.name === selectedRoleName);

  if (!selectedRole) {
    throw Error(`Role assigment failed, because role with name ${selectedRoleName ?? '(undefined)'} does not exist`);
  }

  setIsSubmitting(true);

  onSubmit(selectedRole).then(() => {
    setSelectedRoleName();
    setIsSubmitting(false);
  });
};

const RolesSelector = ({ assignedRolesNames, onSubmit }: Props) => {
  const [roles, setRoles] = useState(Immutable.List());
  const [selectedRoleName, setSelectedRoleName] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const _onSubmit = () => _assignRole(selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting);
  const options = _options(roles, assignedRolesNames);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited)
      .then((paginatedRoles: ?PaginatedListType) => paginatedRoles && setRoles(paginatedRoles.list));
  }, [assignedRolesNames]);

  return (
    <div>
      <FormElements>
        <StyledSelect inputProps={{ 'aria-label': 'Search for roles' }}
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
