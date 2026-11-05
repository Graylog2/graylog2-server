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
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import useCurrentUser from 'hooks/useCurrentUser';
import { LinkContainer, OverlayTrigger, IfPermitted } from 'components/common';
import type UserOverview from 'logic/users/UserOverview';
import UsersDomain from 'domainActions/users/UsersDomain';
import Routes from 'routing/Routes';
import { Button, MenuItem, ButtonToolbar, DeleteMenuItem } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { MoreActions } from 'components/common/EntityDataTable';

type Props = {
  user: UserOverview;
  onDeleted?: () => void;
  onStatusChange?: () => void;
};

const ActionsWrapper = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
`;

const EditTokensAction = ({
  user: { fullName, id },
  wrapperComponent: WrapperComponent,
}: {
  user: UserOverview;
  wrapperComponent: React.ComponentType<any>;
}) => {
  const { t } = useTranslation('users');

  return (
    <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(id)}>
      <WrapperComponent
        id={`edit-tokens-${id}`}
        bsSize="xs"
        title={t('users_list.actions.edit_tokens_title', { name: fullName })}>
        {t('users_list.actions.edit_tokens')}
      </WrapperComponent>
    </LinkContainer>
  );
};

const ReadOnlyActions = ({ user }: { user: UserOverview }) => {
  const { t } = useTranslation('users');
  const tooltip = <>{t('users_list.actions.system_user_tooltip')}</>;

  return (
    <>
      <OverlayTrigger placement="left" overlay={tooltip} trigger={['hover']}>
        <Button bsSize="xs" bsStyle="info" disabled>
          {t('users_list.actions.system_user')}
        </Button>
      </OverlayTrigger>
      <EditTokensAction user={user} wrapperComponent={Button} />
    </>
  );
};

const EditActions = ({
  user,
  user: { username, id, fullName, accountStatus, external, readOnly },
  onDeleted,
  onStatusChange,
}: {
  user: UserOverview;
  onDeleted: () => void;
  onStatusChange: () => void;
}) => {
  const { t } = useTranslation('users');
  const currentUser = useCurrentUser();
  const sendTelemetry = useSendTelemetry();

  const _toggleStatus = () => {
    if (accountStatus === 'enabled') {
      sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_DISABLED, {
        app_action_value: 'user-item-disable',
      });

      // eslint-disable-next-line no-alert
      if (window.confirm(t('users_list.actions.confirm_disable', { name: fullName }))) {
        UsersDomain.setStatus(id, 'disabled').then(() => onStatusChange());
      }

      return;
    }

    UsersDomain.setStatus(id, 'enabled').then(() => onStatusChange());

    sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_ENABLED, {
      app_action_value: 'user-item-enable',
    });
  };

  const _deleteUser = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_DELETED, {
      app_action_value: 'user-item-delete',
    });

    // eslint-disable-next-line no-alert
    if (window.confirm(t('users_list.actions.confirm_delete', { name: fullName }))) {
      UsersDomain.delete(id, fullName).then(() => onDeleted());
    }
  };

  const showEnableDisable = !external && !readOnly && currentUser?.id !== id;

  return (
    <>
      <IfPermitted permissions={[`users:edit:${username}`]}>
        <LinkContainer to={Routes.SYSTEM.USERS.edit(id)}>
          <Button id={`edit-user-${id}`} bsSize="xs" title={t('users_list.actions.edit_title', { name: fullName })}>
            {t('users_list.actions.edit')}
          </Button>
        </LinkContainer>
      </IfPermitted>
      <MoreActions>
        <IfPermitted permissions={[`users:tokenlist:${username}`]}>
          <EditTokensAction user={user} wrapperComponent={MenuItem} />
        </IfPermitted>
        <IfPermitted permissions={[`users:edit:${username}`]}>
          {showEnableDisable && (
            <MenuItem
              id={`set-status-user-${id}`}
              onClick={_toggleStatus}
              title={t('users_list.actions.set_status_title', { name: fullName })}>
              {accountStatus === 'enabled' ? t('users_list.actions.disable') : t('users_list.actions.enable')}
            </MenuItem>
          )}
          <DeleteMenuItem
            id={`delete-user-${id}`}
            title={t('users_list.actions.delete_title', { name: fullName })}
            onClick={_deleteUser}
          />
        </IfPermitted>
      </MoreActions>
    </>
  );
};

const ActionsCell = ({ user, onDeleted = () => {}, onStatusChange = () => {} }: Props) => (
  <td>
    <ActionsWrapper>
      {user.readOnly ? (
        <ReadOnlyActions user={user} />
      ) : (
        <EditActions user={user} onDeleted={onDeleted} onStatusChange={onStatusChange} />
      )}
    </ActionsWrapper>
  </td>
);

export default ActionsCell;
