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
// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';

import withParams from 'routing/withParams';
import { LinkContainer } from 'components/graylog/router';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import RoleDetails from 'components/roles/RoleDetails';
import RoleActionLinks from 'components/roles/navigation/RoleActionLinks';
import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import { Button } from 'components/graylog';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';
import Role from 'logic/roles/Role';

type Props = {
  params: {
    roleId: string,
  },
};

const PageTitle = ({ fullName }: { fullName: string | undefined | null }) => (
  <>
    Role Details {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const RoleDetailsPage = ({ params }: Props) => {
  const [loadedRole, setLoadedRole] = useState<Role | undefined>();
  const roleId = params?.roleId;

  useEffect(() => {
    AuthzRolesDomain.load(roleId).then(setLoadedRole);
  }, [roleId]);

  return (
    <DocumentTitle title={`Role Details ${loadedRole?.name ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedRole?.name} />}
                  subactions={<RoleActionLinks roleId={roleId} />}>
        <span>
          Overview of details like name, description and assigned users.
        </span>
        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>
        <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
          <Button bsStyle="info">Roles Overview</Button>
        </LinkContainer>
      </PageHeader>
      <RoleDetails role={roleId === loadedRole?.id ? loadedRole : undefined} />
    </DocumentTitle>
  );
};

export default withParams(RoleDetailsPage);
