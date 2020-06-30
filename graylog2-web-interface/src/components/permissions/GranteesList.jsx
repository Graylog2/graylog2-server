// @flow strict
import * as React from 'react';

import type { GRN } from 'logic/permissions/types';
import EntityShareState, { type AvailableRoles, type SelectedGrantees } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Role from 'logic/permissions/Role';

import GranteesListItem from './GranteesListItem';

type Props = {
  availableRoles: AvailableRoles,
  entityGRN: GRN,
  selectedGrantees: SelectedGrantees,
  className?: string,
  onRoleChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    roleId: $PropertyType<Role, 'id'>,
  }) => Promise<EntityShareState>,
  onDelete: (GRN) => Promise<EntityShareState>,
};

const GranteesList = ({ onDelete, onRoleChange, entityGRN, availableRoles, selectedGrantees, className }: Props) => {
  return (
    <div className={className}>
      {selectedGrantees.map((grantee) => (
        <GranteesListItem availableRoles={availableRoles}
                          grantee={grantee}
                          entityGRN={entityGRN}
                          key={grantee.id}
                          onRoleChange={onRoleChange}
                          onDelete={onDelete} />
      ))}
    </div>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
