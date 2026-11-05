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
import { LinkContainer, PageHeader, DocumentTitle } from 'components/common';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import RoleDetails from 'components/roles/RoleDetails';
import RoleActionLinks from 'components/roles/navigation/RoleActionLinks';
import DocsHelper from 'util/DocsHelper';
import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import type Role from 'logic/roles/Role';

type Props = {
  params: {
    roleId: string;
  };
};

const PageTitle = ({ fullName }: { fullName: string | undefined | null }) => {
  const { t } = useTranslation('users');

  return (
    <>
      {t('role_details.title_prefix')}{' '}
      {fullName && (
        <>
          - <i>{fullName}</i>
        </>
      )}
    </>
  );
};

const RoleDetailsPage = ({ params }: Props) => {
  const { t } = useTranslation('users');
  const [loadedRole, setLoadedRole] = useState<Role | undefined>();
  const roleId = params?.roleId;

  useEffect(() => {
    AuthzRolesDomain.load(roleId).then(setLoadedRole);
  }, [roleId]);

  return (
    <DocumentTitle title={t('role_details.title', { name: loadedRole?.name ?? '' })}>
      <PageHeader
        title={<PageTitle fullName={loadedRole?.name} />}
        actions={<RoleActionLinks roleId={roleId} />}
        documentationLink={{
          title: t('role_details.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}
        topActions={
          <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
            <Button bsStyle="info">{t('role_details.button_text')}</Button>
          </LinkContainer>
        }>
        <span>{t('role_details.description')}</span>
      </PageHeader>
      <RoleDetails role={roleId === loadedRole?.id ? loadedRole : undefined} />
    </DocumentTitle>
  );
};

export default withParams(RoleDetailsPage);
