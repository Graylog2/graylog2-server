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
import React, {useMemo, useCallback} from 'react';

import {
  DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES, COLUMNS_ORDER,
} from 'components/users/UsersTokenManagement/constants';
import { fetchTokens, keyFn } from 'components/users/UsersTokenManagement/hooks/useTokens';
import type { Token } from 'components/users/UsersTokenManagement/hooks/useTokens';
import { PaginatedEntityTable } from 'components/common';
import TokenActions from 'components/users/UsersTokenManagement/TokenManagementActions';

import CustomColumnRenderers from './ColumnRenderers';


const TokenManagement = () => {
  const tokenAction = useCallback(
    ({ user_id, id:tokenId, NAME:tokenName}: Token) => (
      <TokenActions userId={user_id} tokenId={tokenId} tokenName={tokenName} />
    ),
    [],
  );
  const columnRenderers = useMemo(() => CustomColumnRenderers(), []);

  return (
    <PaginatedEntityTable<Token> humanName="token management"
                                    columnsOrder={COLUMNS_ORDER}
                                    additionalAttributes={ADDITIONAL_ATTRIBUTES}
                                    actionsCellWidth={320}
                                    entityActions={tokenAction}
                                    tableLayout={DEFAULT_LAYOUT}
                                    fetchEntities={fetchTokens}
                                    keyFn={keyFn}
                                    entityAttributesAreCamelCase={false}
                                    columnRenderers={columnRenderers} />
  );
};

export default TokenManagement;
