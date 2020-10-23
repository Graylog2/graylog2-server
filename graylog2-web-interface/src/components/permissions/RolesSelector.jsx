// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { compact } from 'lodash';
import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import Role from 'logic/roles/Role';
import { Button } from 'components/graylog';
import { Select } from 'components/common';

type Props = {
  assignedRolesIds: Immutable.Set<string>,
  identifier: (role: Role) => string,
  onSubmit: (role: Immutable.Set<Role>) => Promise<void>,
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

const _options = (roles: Immutable.Set<Role>, assignedRolesIds, identifier) => roles
  .filter((role) => !assignedRolesIds.includes(identifier(role)))
  .toArray()
  .map((r) => ({ label: r.name, value: r.name, role: r }));

const _assignRole = (selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting) => {
  if (!selectedRoleName) {
    return;
  }

  const selectedRoleNames = selectedRoleName.split(',');

  const selectedRoles = Immutable.Set(compact(selectedRoleNames.map((selection) => {
    return roles.find((r) => r.name === selection);
  })));

  if (selectedRoles.size <= 0) {
    throw Error(`Role assigment failed, because the roles ${selectedRoleName ?? '(undefined)'} does not exist`);
  }

  setIsSubmitting(true);

  onSubmit(selectedRoles).then(() => {
    setSelectedRoleName();
    setIsSubmitting(false);
  });
};

const RolesSelector = ({ assignedRolesIds, onSubmit, identifier }: Props) => {
  const [roles, setRoles] = useState(Immutable.Set());
  const [selectedRoleName, setSelectedRoleName] = useState();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const _onSubmit = () => _assignRole(selectedRoleName, roles, onSubmit, setSelectedRoleName, setIsSubmitting);
  const options = _options(roles, assignedRolesIds, identifier);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];

    AuthzRolesDomain.loadRolesPaginated(...getUnlimited)
      .then((paginatedRoles) => setRoles(Immutable.Set(paginatedRoles.list)));
  }, [assignedRolesIds]);

  return (
    <div>
      <FormElements>
        <StyledSelect inputProps={{ 'aria-label': 'Search for roles' }}
                      onChange={setSelectedRoleName}
                      optionRenderer={_renderRoleOption}
                      options={options}
                      placeholder="Search for roles"
                      multi
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
