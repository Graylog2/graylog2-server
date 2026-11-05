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
import React, { useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import { DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES } from 'components/users/UsersTokenManagement/constants';
import { Row, Col } from 'components/bootstrap';
import { fetchTokens, keyFn } from 'components/users/UsersTokenManagement/hooks/useTokens';
import type { Token } from 'components/users/UsersTokenManagement/hooks/useTokens';
import { PaginatedEntityTable, QueryHelper } from 'components/common';
import TokenActions from 'components/users/UsersTokenManagement/TokenManagementActions';

import CustomColumnRenderers from './ColumnRenderers';

const Container = styled.div`
  .data-table {
    overflow-x: visible;
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const EXAMPLE_USER_QUERY = 'username:givenName';
const EXAMPLE_NAME_QUERY = 'name:tokenName';

const TokenManagement = () => {
  const { t } = useTranslation('users');
  const tokenAction = useCallback(
    ({ user_id, username, id: tokenId, NAME: tokenName }: Token) => (
      <TokenActions userId={user_id} username={username} tokenId={tokenId} tokenName={tokenName} />
    ),
    [],
  );
  const columnRenderers = useMemo(() => CustomColumnRenderers(), []);
  const fieldMap = {
    username: t('token_admin.fields.username'),
    name: t('token_admin.fields.name'),
    created: t('token_admin.fields.created'),
    last_access: t('token_admin.fields.last_access'),
    external_user: t('token_admin.fields.external_user'),
    auth_backend: t('token_admin.fields.auth_backend'),
  };
  const tokenExamples = (
    <>
      <p>
        {t('token_admin.example_user')}
        <br />
        <code>{EXAMPLE_USER_QUERY}</code>
        <br />
      </p>
      <p>
        {t('token_admin.example_name')}
        <br />
        <code>{EXAMPLE_NAME_QUERY}</code>
        <br />
      </p>
    </>
  );

  return (
    <Container>
      <Row className="content">
        <Col xs={12}>
          <Header>
            <h2>{t('token_admin.title')}</h2>
          </Header>
          <PaginatedEntityTable<Token>
            humanName={t('token_admin.human_name')}
            queryHelpComponent={
              <QueryHelper entityName="token" commonFields={[]} fieldMap={fieldMap} example={tokenExamples} />
            }
            additionalAttributes={ADDITIONAL_ATTRIBUTES}
            entityActions={tokenAction}
            tableLayout={DEFAULT_LAYOUT}
            fetchEntities={fetchTokens}
            keyFn={keyFn}
            entityAttributesAreCamelCase={false}
            columnRenderers={columnRenderers}
          />
        </Col>
      </Row>
    </Container>
  );
};

export default TokenManagement;
