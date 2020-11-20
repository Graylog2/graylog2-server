/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/* eslint-disable no-alert */
import * as React from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { LinkContainer, Link } from 'components/graylog/router';
import StringUtils from 'util/StringUtils';
import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { TextOverflowEllipsis } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';

type Props = {
  authenticationBackend: AuthenticationBackend,
  isActive: boolean,
  roles: Immutable.List<Role>,
};

const StyledButtonToolbar = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const DescriptionCell = styled.td`
  max-width: 300px;
`;

const rolesList = (defaultRolesIds: Immutable.List<string>, roles: Immutable.List<Role>) => {
  const defaultRolesNames = defaultRolesIds.map((roleId) => roles.find((role) => role.id === roleId)?.name ?? 'Role not found');

  return defaultRolesNames.join(', ');
};

const EditButton = ({ authenticationBackend }: { authenticationBackend: AuthenticationBackend }) => {
  const link = Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(authenticationBackend.id);

  return (
    <LinkContainer to={link}>
      <Button bsStyle="info" bsSize="xs" type="button">
        Edit
      </Button>
    </LinkContainer>
  );
};

const confirmMessage = (authBackendTitle: string, actionName: string) => {
  return `Do you really want to ${actionName} the authentication service "${StringUtils.truncateWithEllipses(authBackendTitle, 30)}"`;
};

const ActionsCell = ({ isActive, authenticationBackend }: { authenticationBackend: AuthenticationBackend, isActive: boolean }) => {
  const { title, id } = authenticationBackend;
  const _setActiveBackend = (backendId) => AuthenticationDomain.setActiveBackend(backendId, title);

  const _deactivateBackend = () => {
    if (window.confirm(confirmMessage(title, 'deactivate'))) {
      _setActiveBackend(null);
    }
  };

  const _activateBackend = () => {
    if (window.confirm(confirmMessage(title, 'activate'))) {
      _setActiveBackend(id);
    }
  };

  const _deleteBackend = () => {
    if (window.confirm(confirmMessage(title, 'delete'))) {
      AuthenticationDomain.delete(id, title);
    }
  };

  return (
    <td className="limited">
      <StyledButtonToolbar>
        {isActive ? (
          <>
            <Button onClick={_deactivateBackend} bsStyle="warning" bsSize="xs" type="button">
              Deactivate
            </Button>
            <EditButton authenticationBackend={authenticationBackend} />
          </>
        ) : (
          <>
            <Button onClick={_activateBackend} bsStyle="warning" bsSize="xs" type="button">
              Activate
            </Button>
            <EditButton authenticationBackend={authenticationBackend} />
            <Button onClick={_deleteBackend} bsStyle="danger" bsSize="xs" type="button">
              Delete
            </Button>
          </>
        )}
      </StyledButtonToolbar>
    </td>
  );
};

const BackendsOverviewItem = ({ authenticationBackend, isActive, roles }: Props) => {
  const { title, description, defaultRoles, id } = authenticationBackend;
  const detailsLink = isActive ? Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE : Routes.SYSTEM.AUTHENTICATION.BACKENDS.show(id);

  return (
    <tr key={id} className={isActive ? 'active' : ''}>
      <td className="limited">
        <Link to={detailsLink}>
          {title}
        </Link>
      </td>
      <DescriptionCell>
        <TextOverflowEllipsis>
          {description}
        </TextOverflowEllipsis>
      </DescriptionCell>
      <td className="limited">
        {rolesList(defaultRoles, roles)}
      </td>
      <ActionsCell authenticationBackend={authenticationBackend} isActive={isActive} />
    </tr>
  );
};

export default BackendsOverviewItem;
