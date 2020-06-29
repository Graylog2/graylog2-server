// @flow strict
import * as React from 'react';

import type { GRN } from 'logic/permissions/types';
import type { AvailableGrantees, ActiveShares, AvailableRoles, SelectedGranteeRoles } from 'logic/permissions/EntityShareState';

import GranteesListItem from './GranteesListItem';

const _userLookup = (userId: GRN, grantees: AvailableGrantees) => grantees.find((grantee) => grantee.id === userId);

type Props = {
  activeShares: ActiveShares,
  availableRoles: AvailableRoles,
  availableGrantees: AvailableGrantees,
  entityGRN: GRN,
  selectedGranteeRoles: SelectedGranteeRoles,
  className?: string,
};

const GranteesList = ({ entityGRN, activeShares, availableRoles, availableGrantees, selectedGranteeRoles, className }: Props) => {
  return (
    <div className={className}>
      {activeShares.map(({ grantee: granteeId, role: roleId }) => {
        const grantee = _userLookup(granteeId, availableGrantees);

        return grantee && (
          <GranteesListItem availableRoles={availableRoles}
                            key={grantee.id}
                            grantee={grantee}
                            entityGRN={entityGRN}
                            granteeRoleId={roleId} />
        );
      })}
      {selectedGranteeRoles.entrySeq().map(([granteeId, roleId]) => {
        const grantee = _userLookup(granteeId, availableGrantees);

        return grantee && (
          <GranteesListItem availableRoles={availableRoles}
                            grantee={grantee}
                            entityGRN={entityGRN}
                            key={grantee.id}
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
