import * as React from 'react';
import { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import withParams from 'routing/withParams';
import { Row, Col } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { PageHeader, DocumentTitle, Spinner } from 'components/common';
import TokenList from 'components/users/TokenList';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
import useBasicUser from 'hooks/useBasicUser';

type Props = {
  params: {
    userId: string;
  };
};

const PageTitle = ({ fullName }: { fullName: string | null | undefined }) => {
  const { t } = useTranslation('users');

  return (
    <>
      {t('user_tokens_edit.title_prefix')}{' '}
      {fullName && (
        <>
          - <i>{fullName}</i>
        </>
      )}
    </>
  );
};

const _loadTokens = (loadedUser, setTokens) => {
  if (loadedUser) {
    UsersDomain.loadTokens(loadedUser.id).then(setTokens);
  }
};

const _createToken = (tokenName, userId, loadTokens, setCreatingToken, tokenTtl) => {
  const promise = UsersDomain.createToken(userId, tokenName, tokenTtl);

  setCreatingToken(true);

  return promise
    .then((token) => {
      loadTokens();

      return token;
    })
    .finally(() => {
      setCreatingToken(false);
    });
};

const UserEditPage = ({ params }: Props) => {
  const { t } = useTranslation('users');
  const [tokens, setTokens] = useState([]);
  const [creatingToken, setCreatingToken] = useState(false);

  const userId = params?.userId;

  const { data: loadedUser } = useBasicUser(userId, { enabled: !!userId });

  const loadTokens = useCallback(() => _loadTokens(loadedUser, setTokens), [loadedUser]);
  const _handleTokenCreate = ({ tokenName, tokenTtl }: { tokenName: string; tokenTtl: string }) =>
    _createToken(tokenName, userId, loadTokens, setCreatingToken, tokenTtl);

  useEffect(() => {
    loadTokens();
  }, [loadTokens, loadedUser]);

  return (
    <DocumentTitle title={t('user_tokens_edit.title', { username: loadedUser?.fullName ?? '' })}>
      <UsersPageNavigation />
      <PageHeader
        title={<PageTitle fullName={loadedUser?.fullName} />}
        actions={
          <UserActionLinks
            userId={userId}
            username={loadedUser?.username ?? ''}
            userIsReadOnly={loadedUser?.readOnly ?? false}
          />
        }
        documentationLink={{
          title: t('user_tokens_edit.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('user_tokens_edit.description')}</span>
      </PageHeader>

      <Row className="content">
        <Col lg={12}>
          {loadedUser ? (
            <TokenList
              tokens={tokens}
              user={loadedUser}
              onCreate={_handleTokenCreate}
              creatingToken={creatingToken}
              onDelete={loadTokens}
            />
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
