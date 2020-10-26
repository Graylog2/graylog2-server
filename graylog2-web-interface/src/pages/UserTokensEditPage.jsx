// @flow strict
import * as React from 'react';
import { useEffect, useState, useContext, useCallback } from 'react';

import withParams from 'routing/withParams';
import { Row, Col } from 'components/graylog';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { isPermitted } from 'util/PermissionsMixin';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { PageHeader, DocumentTitle } from 'components/common';
import { Headline } from 'components/common/Section/SectionComponent';
import TokenList from 'components/users/TokenList';
import UserOverviewLinks from 'components/users/navigation/UserOverviewLinks';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
import DocumentationLink from 'components/support/DocumentationLink';

type Props = {
  params: {
    userId: string,
  },
};

const PageTitle = ({ fullName }: {fullName: ?string}) => (
  <>
    Edit Tokens Of User  {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const _loadTokens = (userId, currentUser, setTokens) => {
  if (isPermitted(currentUser?.permissions, [`users:tokenlist:${userId}`])) {
    UsersDomain.loadTokens(userId).then(setTokens);
  } else {
    setTokens([]);
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

  promise.then(() => {
    loadTokens();
    setCreatingToken(false);
  });
};

const UserEditPage = ({ params }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const [loadedUser, setLoadedUser] = useState();
  const [tokens, setTokens] = useState([]);
  const [deletingTokenId, setDeletingTokenId] = useState();
  const [creatingToken, setCreatingToken] = useState(false);

  const userId = params?.userId;
  const loadTokens = useCallback(() => _loadTokens(userId, currentUser, setTokens), [userId, currentUser]);

  const _handleTokenDelete = (tokenId, tokenName) => _deleteToken(tokenId, tokenName, userId, loadTokens, setDeletingTokenId);
  const _handleTokenCreate = (tokenName) => _createToken(tokenName, userId, loadTokens, setCreatingToken);

  useEffect(() => {
    loadTokens();

    UsersDomain.load(userId).then((newLoadedUser) => newLoadedUser && setLoadedUser(newLoadedUser));
  }, [currentUser, userId, loadTokens]);

  return (
    <DocumentTitle title={`Edit Tokens Of User ${loadedUser?.fullName ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}
                  subactions={(
                    <UserActionLinks userId={userId}
                                     userIsReadOnly={loadedUser?.readOnly ?? false} />
                  )}>
        <span>
          You can create new tokens or delete old ones.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserOverviewLinks />
      </PageHeader>

      <Row className="content">
        <Col lg={8}>
          <Headline>Create And Edit Tokens</Headline>
          <TokenList tokens={tokens}
                     onDelete={_handleTokenDelete}
                     onCreate={_handleTokenCreate}
                     creatingToken={creatingToken}
                     deletingToken={deletingTokenId} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default withParams(UserEditPage);
