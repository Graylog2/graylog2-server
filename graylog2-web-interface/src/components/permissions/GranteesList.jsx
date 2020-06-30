// @flow strict
import * as React from 'react';

import type { GRN } from 'logic/permissions/types';
import EntityShareState, { type AvailableGrantees, type AvailableRoles, type SelectedGranteeRoles } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Role from 'logic/permissions/Role';

import GranteesListItem from './GranteesListItem';

type Props = {
  availableRoles: AvailableRoles,
  availableGrantees: AvailableGrantees,
  entityGRN: GRN,
  selectedGranteeRoles: SelectedGranteeRoles,
  className?: string,
  onRoleChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    roleId: $PropertyType<Role, 'id'>,
  }) => Promise<EntityShareState>,
  onDelete: (GRN) => Promise<EntityShareState>,
};

const GranteesList = ({ onDelete, onRoleChange, entityGRN, availableRoles, availableGrantees, selectedGranteeRoles, className }: Props) => {
  return (
    <div className={className}>
      {selectedGranteeRoles.entrySeq().map(([granteeId, roleId]) => {
        const grantee = _userLookup(granteeId, availableGrantees);

        return grantee && (
          <GranteesListItem availableRoles={availableRoles}
                            grantee={grantee}
                            entityGRN={entityGRN}
                            key={grantee.id}
                            onRoleChange={onRoleChange}
                            onDelete={onDelete}
                            granteeRoleId={roleId} />
        );
      })}
    </div>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
