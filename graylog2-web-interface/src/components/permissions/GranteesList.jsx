// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import EntityShareState, { type AvailableRoles, type SelectedGrantees } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Role from 'logic/permissions/Role';
import { type ThemeInterface } from 'theme';

import GranteesListItem from './GranteesListItem';

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  >:nth-child(even) {
    background: ${theme.utils.colorLevel(theme.colors.global.tableBackgroundAlt, -4)};
  };
  >:nth-child(odd) {
    background: ${theme.colors.global.tableBackground};
  };
`);

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
    <Container className={className}>
      {selectedGrantees.map((grantee) => (
        <GranteesListItem availableRoles={availableRoles}
                          grantee={grantee}
                          entityGRN={entityGRN}
                          key={grantee.id}
                          onRoleChange={onRoleChange}
                          onDelete={onDelete} />
      ))}
    </Container>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
