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
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

import withParams from 'routing/withParams';
import { PageHeader, DocumentTitle } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import UserDetails from 'components/users/UserDetails';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
import type User from 'logic/users/User';

type Props = {
  params: {
    userId: string;
  };
};

const PageTitle = ({ fullName }: { fullName: string }) => {
  const { t } = useTranslation('users');

  return fullName ? (
    <>
      {t('user_details_page.title')} - <i>{fullName}</i>
    </>
  ) : (
    <>{t('user_details_page.title')}</>
  );
};

const UserDetailsPage = ({ params }: Props) => {
  const { t } = useTranslation('users');
  const [loadedUser, setLoadedUser] = useState<User | undefined>();
  const userId = params?.userId;

  useEffect(() => {
    UsersDomain.load(userId).then(setLoadedUser);
  }, [userId]);

  return (
    <DocumentTitle title={t('user_details_page.title_with_name', { name: loadedUser?.fullName ?? '' })}>
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
          title: t('user_details_page.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('user_details_page.description')}</span>
      </PageHeader>

      <UserDetails user={userId === loadedUser?.id ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withParams(UserDetailsPage);
