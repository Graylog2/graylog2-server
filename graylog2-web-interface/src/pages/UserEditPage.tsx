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
import { useQuery } from '@tanstack/react-query';

import withParams from 'routing/withParams';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { USERS_QUERY_KEY } from 'hooks/useUsers';
import { PageHeader, DocumentTitle } from 'components/common';
import UserEdit from 'components/users/UserEdit';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import UserActionLinks from 'components/users/navigation/UserActionLinks';

type Props = {
  params: {
    userId: string;
  };
};

const PageTitle = ({ fullName }: { fullName: string | null | undefined }) => {
  const { t } = useTranslation('users');
  return (
    <>
      {t('user_edit.title_prefix')}{' '}
      {fullName && (
        <>
          - <i>{fullName}</i>
        </>
      )}
    </>
  );
};

const UserEditPage = ({ params }: Props) => {
  const { t } = useTranslation('users');
  const userId = params?.userId;

  // Child components (e.g. the roles section) trigger a user state update by invalidating this query.
  const { data: loadedUser } = useQuery({
    queryKey: [...USERS_QUERY_KEY, userId],
    queryFn: () => UsersDomain.load(userId),
    retry: false,
  });

  const fullName = loadedUser?.fullName ?? '';
  const readOnly = loadedUser?.readOnly ?? false;
  const userToEdit = userId === loadedUser?.id ? loadedUser : undefined;

  return (
    <DocumentTitle title={t('user_edit.title', { username: fullName })}>
      <UsersPageNavigation />
      <PageHeader
        title={<PageTitle fullName={fullName} />}
        actions={<UserActionLinks userId={userId} username={loadedUser?.username ?? ''} userIsReadOnly={readOnly} />}
        documentationLink={{
          title: t('user_edit.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('user_edit.description')}</span>
      </PageHeader>
      <UserEdit user={userToEdit} />
    </DocumentTitle>
  );
};

export default withParams(UserEditPage);
