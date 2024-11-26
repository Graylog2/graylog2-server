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
import * as React from 'react';
import styled from 'styled-components';

import useCurrentUser from 'hooks/useCurrentUser';
import { LinkContainer } from 'components/common/router';
import type UserOverview from 'logic/users/UserOverview';
import UsersDomain from 'domainActions/users/UsersDomain';
import Routes from 'routing/Routes';
import { Button, MenuItem, ButtonToolbar, DeleteMenuItem } from 'components/bootstrap';
import { OverlayTrigger, IfPermitted } from 'components/common';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { MoreActions } from 'components/common/EntityDataTable';

type Props = {
  user: UserOverview,
};

const ActionsWrapper = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const EditTokensAction = ({
  user: { fullName, id },
  wrapperComponent: WrapperComponent,
}: {
  user: UserOverview,
  wrapperComponent: React.ComponentType<any>,
}) => (
  <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(id)}>
    <WrapperComponent id={`edit-tokens-${id}`}
                      bsSize="xs"
                      title={`Edit tokens of user ${fullName}`}>
      Edit tokens
    </WrapperComponent>
  </LinkContainer>
);

const ReadOnlyActions = ({ user }: { user: UserOverview }) => {
  const tooltip = (
    <>
      System users can only be modified in the Graylog configuration file.
    </>
  );

  return (
    <>
      <OverlayTrigger placement="left" overlay={tooltip} trigger={['hover']}>
        <Button bsSize="xs" bsStyle="info" disabled>System user</Button>
      </OverlayTrigger>
      <EditTokensAction user={user} wrapperComponent={Button} />
    </>
  );
};

const EditActions = ({ user, user: { username, id, fullName, accountStatus, external, readOnly } }: {
  user: UserOverview
}) => {
  const currentUser = useCurrentUser();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const _toggleStatus = () => {
    if (accountStatus === 'enabled') {
      sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_DISABLED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_action_value: 'user-item-disable',
      });

      // eslint-disable-next-line no-alert
      if (window.confirm(`Do you really want to disable user ${fullName}? All current sessions will be terminated.`)) {
        UsersDomain.setStatus(id, 'disabled');
      }

      return;
    }

    UsersDomain.setStatus(id, 'enabled');

    sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_ENABLED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'user-item-enable',
    });
  };

  const _deleteUser = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_DELETED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_action_value: 'user-item-delete',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete user ${fullName}?`)) {
      UsersDomain.delete(id, fullName);
    }
  };

  const showEnableDisable = !external && !readOnly && currentUser?.id !== id;

  return (
    <>
      <IfPermitted permissions={[`users:edit:${username}`]}>
        <LinkContainer to={Routes.SYSTEM.USERS.edit(id)}>
          <Button id={`edit-user-${id}`} bsSize="xs" title={`Edit user ${fullName}`}>
            Edit
          </Button>
        </LinkContainer>
      </IfPermitted>
      <MoreActions>
        <EditTokensAction user={user} wrapperComponent={MenuItem} />
        <IfPermitted permissions={[`users:edit:${username}`]}>
          {showEnableDisable && (
            <MenuItem id={`set-status-user-${id}`}
                      onClick={_toggleStatus}
                      title={`Set new account status for ${fullName}`}>
              {accountStatus === 'enabled' ? 'Disable' : 'Enable'}
            </MenuItem>
          )}
          <DeleteMenuItem id={`delete-user-${id}`}
                          title={`Delete user ${fullName}`}
                          onClick={_deleteUser} />
        </IfPermitted>
      </MoreActions>
    </>
  );
};

const ActionsCell = ({ user }: Props) => (
  <td>
    <ActionsWrapper>
      {user.readOnly ? (
        <ReadOnlyActions user={user} />
      ) : (
        <EditActions user={user} />
      )}
    </ActionsWrapper>
  </td>
);

export default ActionsCell;
