// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import UserOverview from 'logic/users/UserOverview';
import type { ThemeInterface } from 'theme';

const Td: StyledComponent<{}, ThemeInterface, HTMLTableCellElement> = styled.td`
  max-width: 300px;
`;

const Role = styled.span`
  margin-right: 5px;
  margin-bottom: 5px;
  display: inline-block;
  line-height: 15px;
`;

const RolesCell = ({ roles }: { roles: $PropertyType<UserOverview, 'roles'> }) => (
  <Td>
    {roles.map((role) => (
      <Role key={role} className={`label label-${role === 'Admin' ? 'info' : 'default'}`}>
        {role}
      </Role>
    ))}
  </Td>
);

export default RolesCell;
