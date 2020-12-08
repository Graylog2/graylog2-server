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

import withParams from 'routing/withParams';
import { PageHeader, DocumentTitle } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import UserDetails from 'components/users/UserDetails';
import UserOverviewLinks from 'components/users/navigation/UserOverviewLinks';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
import DocumentationLink from 'components/support/DocumentationLink';
import User from 'logic/users/User';

type Props = {
  params: {
    userId: string,
  },
};

const PageTitle = ({ fullName }: {fullName: string}) => (
  <>
    User Details {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const UserDetailsPage = ({ params }: Props) => {
  const [loadedUser, setLoadedUser] = useState<User | undefined>();
  const userId = params?.userId;

  useEffect(() => {
    UsersDomain.load(userId).then(setLoadedUser);
  }, [userId]);

  return (
    <DocumentTitle title={`User Details ${loadedUser?.fullName ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}
                  subactions={(
                    <UserActionLinks userId={userId}
                                     userIsReadOnly={loadedUser?.readOnly ?? false} />
                  )}>
        <span>
          Overview of details like profile information, settings, teams and roles.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserOverviewLinks />
      </PageHeader>

      <UserDetails user={userId === loadedUser?.id ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withParams(UserDetailsPage);
