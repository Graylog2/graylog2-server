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
import { useEffect, useState, useCallback } from 'react';

import type User from 'logic/users/User';
import withParams from 'routing/withParams';
import { Row, Col } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { PageHeader, DocumentTitle, Spinner } from 'components/common';
import { Headline } from 'components/common/Section/SectionComponent';
import TokenList from 'components/users/TokenList';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
import useCurrentUser from 'hooks/useCurrentUser';

type Props = {
  params: {
    userId: string,
  },
};

const PageTitle = ({ fullName }: { fullName: string | null | undefined }) => (
  <>
    Edit Tokens Of User  {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const _loadTokens = (loadedUser, currentUser, setTokens) => {
  if (loadedUser) {
    if (isPermitted(currentUser?.permissions, [`users:tokenlist:${loadedUser.username}`])) {
      UsersDomain.loadTokens(loadedUser.id).then(setTokens);
    } else {
      setTokens([]);
    }
  }
};

const _deleteToken = (tokenId, tokenName, userId, loadTokens, setDeletingTokenId) => {
  const promise = UsersDomain.deleteToken(userId, tokenId, tokenName);

  setDeletingTokenId(tokenId);

  promise.then(() => {
    loadTokens();
    setDeletingTokenId(undefined);
  });
};

const _createToken = (tokenName, userId, loadTokens, setCreatingToken) => {
  const promise = UsersDomain.createToken(userId, tokenName);

  setCreatingToken(true);

  return promise.then((token) => {
    loadTokens();
    setCreatingToken(false);

    return token;
  });
};

const UserEditPage = ({ params }: Props) => {
  const currentUser = useCurrentUser();
  const [loadedUser, setLoadedUser] = useState<User | undefined>();
  const [tokens, setTokens] = useState([]);
  const [deletingTokenId, setDeletingTokenId] = useState();
  const [creatingToken, setCreatingToken] = useState(false);

  const userId = params?.userId;

  const loadTokens = useCallback(() => _loadTokens(loadedUser, currentUser, setTokens), [currentUser, loadedUser]);
  const _handleTokenDelete = (tokenId, tokenName) => _deleteToken(tokenId, tokenName, userId, loadTokens, setDeletingTokenId);
  const _handleTokenCreate = (tokenName) => _createToken(tokenName, userId, loadTokens, setCreatingToken);

  useEffect(() => { loadTokens(); }, [loadTokens, loadedUser]);
  useEffect(() => { UsersDomain.load(userId).then(setLoadedUser); }, [userId]);

  return (
    <DocumentTitle title={`Edit Tokens Of User ${loadedUser?.fullName ?? ''}`}>
      <UsersPageNavigation />
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}
                  actions={(
                    <UserActionLinks userId={userId}
                                     userIsReadOnly={loadedUser?.readOnly ?? false} />
                  )}
                  documentationLink={{
                    title: 'Permissions documentation',
                    path: DocsHelper.PAGES.USERS_ROLES,
                  }}>
        <span>
          You can create new tokens or delete old ones.
        </span>
      </PageHeader>

      <Row className="content">
        <Col lg={8}>
          <Headline>Create And Edit Tokens</Headline>
          {loadedUser ? (
            <TokenList tokens={tokens}
                       onDelete={_handleTokenDelete}
                       onCreate={_handleTokenCreate}
                       creatingToken={creatingToken}
                       deletingToken={deletingTokenId} />
          ) : (
            <Row>
              <Col xs={12}>
                <Spinner />
              </Col>
            </Row>
          )}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default withParams(UserEditPage);
