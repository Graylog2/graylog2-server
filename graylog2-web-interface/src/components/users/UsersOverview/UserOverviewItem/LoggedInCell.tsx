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

import type UserOverview from 'logic/users/UserOverview';
import { OverlayTrigger, RelativeTime, StatusIcon } from 'components/common';

type Props = {
  lastActivity: UserOverview['lastActivity'];
  clientAddress: UserOverview['clientAddress'];
  sessionActive: UserOverview['sessionActive'];
};

const Td = styled.td`
  width: 35px;
  text-align: right;
  position: relative;
`;

const LoggedInCell = ({ lastActivity, sessionActive, clientAddress }: Props) => {
  const { t } = useTranslation('users');

  return (
    <Td>
      <OverlayTrigger
        trigger={['hover', 'focus']}
        placement="right"
        title={sessionActive ? t('users_list.logged_in.title') : undefined}
        overlay={
          sessionActive ? (
            <>
              <div>
                {t('users_list.logged_in.last_activity', { activity: '' })}
                {lastActivity ? <RelativeTime dateTime={lastActivity} /> : '-'}
              </div>
              <div>{t('users_list.logged_in.client_address', { address: clientAddress ?? '-' })}</div>
            </>
          ) : (
            <>{t('users_list.logged_in.not_logged_in')}</>
          )
        }
        rootClose>
        <StatusIcon active={sessionActive} />
      </OverlayTrigger>
    </Td>
  );
};

export default LoggedInCell;
