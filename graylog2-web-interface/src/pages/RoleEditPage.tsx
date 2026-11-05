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
import RoleEdit from 'components/roles/RoleEdit';
import Routes from 'routing/Routes';
import RoleActionLinks from 'components/roles/navigation/RoleActionLinks';
import { Button } from 'components/bootstrap';
import { loadRole } from 'hooks/useAuthzRoles';
import DocsHelper from 'util/DocsHelper';
import type Role from 'logic/roles/Role';

type Props = {
  params: {
    roleId: string;
  };
};

const PageTitle = ({ name }: { name: string | undefined | null }) => {
  const { t } = useTranslation('users');

  return (
    <>
      {t('role_edit.title_prefix')}{' '}
      {name && (
        <>
          - <i>{name}</i>
        </>
      )}
    </>
  );
};

const RoleEditPage = ({ params }: Props) => {
  const { t } = useTranslation('users');
  const [loadedRole, setLoadedRole] = useState<Role | undefined>();
  const roleId = params?.roleId;

  useEffect(() => {
    loadRole(roleId).then(setLoadedRole);
  }, [roleId]);

  return (
    <DocumentTitle title={t('role_edit.title', { name: loadedRole?.name ?? '' })}>
      <PageHeader
        title={<PageTitle name={loadedRole?.name} />}
        actions={<RoleActionLinks roleId={roleId} />}
        topActions={
          <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
            <Button bsStyle="info">{t('role_edit.button_text')}</Button>
          </LinkContainer>
        }
        documentationLink={{
          title: t('role_edit.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('role_edit.description')}</span>
      </PageHeader>
      <RoleEdit role={roleId === loadedRole?.id ? loadedRole : undefined} />
    </DocumentTitle>
  );
};

export default withParams(RoleEditPage);
