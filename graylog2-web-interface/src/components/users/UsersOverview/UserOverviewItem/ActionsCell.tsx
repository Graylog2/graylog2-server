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
import { Button, Tooltip, DropdownButton, MenuItem, ButtonToolbar } from 'components/bootstrap';
import { OverlayTrigger, IfPermitted } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

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
  wrapperComponent: Button | MenuItem,
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
    <Tooltip id="system-user">System users can only be modified in the Graylog configuration
      file.
    </Tooltip>
  );

  return (
    <>
      <OverlayTrigger placement="left" overlay={tooltip}>
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

  const _toggleStatus = () => {
    if (accountStatus === 'enabled') {
      sendTelemetry('click', {
        app_pathname: 'users',
        app_action_value: 'user-item-disable',
      });

      // eslint-disable-next-line no-alert
      if (window.confirm(`Do you really want to disable user ${fullName}? All current sessions will be terminated.`)) {
        UsersDomain.setStatus(id, 'disabled');
      }

      return;
    }

    UsersDomain.setStatus(id, 'enabled');

    sendTelemetry('click', {
      app_pathname: 'users',
      app_action_value: 'user-item-enable',
    });
  };

  const _deleteUser = () => {
    sendTelemetry('click', {
      app_pathname: 'users',
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
      <DropdownButton bsSize="xs" title="More actions" pullRight id={`delete-user-${id}`}>
        <EditTokensAction user={user} wrapperComponent={MenuItem} />
        <IfPermitted permissions={[`users:edit:${username}`]}>
          {showEnableDisable && (
            <MenuItem id={`set-status-user-${id}`}
                      onClick={_toggleStatus}
                      title={`Set new account status for ${fullName}`}>
              {accountStatus === 'enabled' ? 'Disable' : 'Enable'}
            </MenuItem>
          )}
          <MenuItem id={`delete-user-${id}`}
                    bsStyle="primary"
                    bsSize="xs"
                    title={`Delete user ${fullName}`}
                    onClick={_deleteUser}>
            Delete
          </MenuItem>
        </IfPermitted>
      </DropdownButton>
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
