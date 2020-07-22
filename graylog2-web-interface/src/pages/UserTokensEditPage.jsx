// @flow strict
import * as React from 'react';
import { useEffect, useState, useContext } from 'react';
import { withRouter } from 'react-router';

import { Row, Col } from 'components/graylog';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { isPermitted } from 'util/PermissionsMixin';
import DocsHelper from 'util/DocsHelper';
import { useStore } from 'stores/connect';
import { UsersActions, UsersStore } from 'stores/users/UsersStore';
import { PageHeader, DocumentTitle } from 'components/common';
import { Headline } from 'components/users/SectionComponent';
import TokenList from 'components/users/TokenList';
import UserManagementLinks from 'components/users/UserManagementLinks';
import DocumentationLink from 'components/support/DocumentationLink';

type Props = {
  params: {
    username: string,
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

const _loadTokens = (username, currentUser, setTokens) => {
  if (isPermitted(currentUser?.permissions, [`users:tokenlist:${username}`])) {
    UsersActions.loadTokens(username).then((tokens) => {
      setTokens(tokens);
    });
  } else {
    setTokens([]);
  }
};

const _deleteToken = (tokenId, tokenName, username, loadTokens, setDeletingTokenId) => {
  const promise = UsersActions.deleteToken(username, tokenId, tokenName);

  setDeletingTokenId(tokenId);

  promise.then(() => {
    loadTokens();
    setDeletingTokenId(undefined);
  });
};

const _createToken = (tokenName, username, loadTokens, setCreatingToken) => {
  const promise = UsersActions.createToken(username, tokenName);

  setCreatingToken(true);

  promise.then(() => {
    loadTokens();
    setCreatingToken(false);
  });
};

const UserEditPage = ({ params }: Props) => {
  const { loadedUser } = useStore(UsersStore);
  const currentUser = useContext(CurrentUserContext);
  const [tokens, setTokens] = useState([]);
  const [deletingTokenId, setDeletingTokenId] = useState();
  const [creatingToken, setCreatingToken] = useState(false);

  const username = params?.username;
  const loadTokens = () => _loadTokens(params?.username, currentUser, setTokens);

  const _handleTokenDelete = (tokenId, tokenName) => _deleteToken(tokenId, tokenName, username, loadTokens, setDeletingTokenId);
  const _handleTokenCreate = (tokenName) => _createToken(tokenName, username, loadTokens, setCreatingToken);

  useEffect(() => {
    loadTokens();
  }, [currentUser, username]);

  return (
    <DocumentTitle title={`Edit Tokens Of User ${loadedUser?.fullName ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}>
        <span>
          You can create new tokens or delete old ones.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserManagementLinks username={username}
                             userIsReadOnly={loadedUser?.readOnly} />
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

export default withRouter(UserEditPage);
