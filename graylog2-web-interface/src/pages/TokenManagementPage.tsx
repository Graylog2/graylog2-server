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

import { PageHeader, DocumentTitle } from 'components/common';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import TokenManagement from 'components/users/UsersTokenManagement/TokenManagement';
import useProductName from 'brand-customization/useProductName';

const TokenManagementPage = () => {
  const { t } = useTranslation('users');
  const productName = useProductName();

  return (
    <DocumentTitle title={t('token_management.title')}>
      <UsersPageNavigation />
      <PageHeader title={t('token_management.title')}>
        <span>{t('token_management.description', { productName })}</span>
      </PageHeader>

      <TokenManagement />
    </DocumentTitle>
  );
};

export default TokenManagementPage;
