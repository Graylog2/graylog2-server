import * as React from 'react';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type {Token} from 'components/users/UsersTokenManagement/hooks/useTokens';
import {Timestamp} from 'components/common';
import IsExternalUserCell from 'components/users/UsersTokenManagement/cells/IsExternalUserCell';

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
      renderCell: (_username: string, token) => token.username,
      width: 0.2,
    },
    token_id: {
      renderCell: (_token_id: string, token) => token.token_id,
      width: 0.3,
    },
    token_name: {
      renderCell: (_token_name: string, token) => token.token_name,
      width: 0.2,
    },
    created_at: {
      renderCell: (_created_at: string, token) => token?.created_at
        && <Timestamp dateTime={token.created_at}/>,
      width: 0.2,
    },
    last_access: {
      renderCell: (_last_access: string, token) => token?.last_access
        && <Timestamp dateTime={token.last_access}/>,
      width: 0.2,
    },
    user_is_external: {
      renderCell: (_user_is_external: boolean, token) => <IsExternalUserCell token={token}/>,
      width: 0.2,
    },
    auth_backend: {
      renderCell: (_auth_backend: string, token) => token.auth_backend ? token.auth_backend : "N/A",
      width: 0.2,
    },
  },
});

export default customColumnRenderers;
