// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { type PaginatedListType } from 'stores/roles/AuthzRolesStore';
import Role from 'logic/roles/Role';
import { Button } from 'components/graylog';
import { Select } from 'components/common';

type Props = {
  assignedRolesIds: Immutable.List<string>,
  identifier: (role: Role) => string,
  onSubmit: (role: Role) => Promise<void>,
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

const _options = (roles: Immutable.List<Role>, assignedRolesIds, identifier) => roles
  .filter((role) => !assignedRolesIds.includes(identifier(role)))
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

const RolesSelector = ({ assignedRolesIds, onSubmit, identifier }: Props) => {
  const [roles, setRoles] = useState(Immutable.List());
  const [selectedRoleName, setSelectedRoleName] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const _onSubmit = () => _assignRole(selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting);
  const options = _options(roles, assignedRolesIds, identifier);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited)
      .then((paginatedRoles: ?PaginatedListType) => paginatedRoles && setRoles(paginatedRoles.list));
  }, [assignedRolesIds]);

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

RolesSelector.defaultProps = {
  identifier: (role) => role.id,
};

RolesSelector.propTypes = {
  identifier: PropTypes.func,
  onSubmit: PropTypes.func.isRequired,
};

export default RolesSelector;
