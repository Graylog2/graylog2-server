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

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { Token } from 'components/users/UsersTokenManagement/hooks/useTokens';
import { Timestamp } from 'components/common';
import IsExternalUserCell from 'components/users/UsersTokenManagement/cells/IsExternalUserCell';
import ErrorPopover from 'components/lookup-tables/ErrorPopover';

const customColumnRenderers = (): ColumnRenderers<Token> => ({
  attributes: {
    id: {
      renderCell: (_id: string, token) => token.id,
      width: 0.2,
    },
    user_id: {
      renderCell: (_user_id: string, token) => token.user_id,
      width: 0.2,
    },
    username: {
      renderCell: (_username: string, token) => <>{token.user_deleted && <ErrorPopover placement="right" errorText="User does not exist." title="Token user error" />}{token.username}</>,
      width: 0.2,
    },
    NAME: {
      renderCell: (_NAME: string, token) => token.NAME,
      width: 0.2,
    },
    created_at: {
      renderCell: (_created_at: string, token) => token?.created_at && <Timestamp dateTime={token.created_at} />,
      width: 0.2,
    },
    last_access: {
      renderCell: (_last_access: string, token) => token?.last_access && <Timestamp dateTime={token.last_access} />,
      width: 0.2,
    },
    external_user: {
      renderCell: (_external_user: boolean, token) => <IsExternalUserCell token={token} />,
      width: 0.2,
    },
    title: {
      renderCell: (_title: string, token) => (token.title ? token.title : 'N/A'),
      width: 0.2,
    },
  },
});

export default customColumnRenderers;
