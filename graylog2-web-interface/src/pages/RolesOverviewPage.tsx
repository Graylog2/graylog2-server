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

import { LinkContainer, PageHeader, DocumentTitle, Icon } from 'components/common';
import RolesOverview from 'components/roles/RolesOverview';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { Button, Row, Col, Alert } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import useProductName from 'brand-customization/useProductName';

const RolesOverviewPage = () => {
  const { t } = useTranslation('users');
  const productName = useProductName();

  return (
    <DocumentTitle title={t('roles_overview.title')}>
      <PageHeader
        title={t('roles_overview.title')}
        documentationLink={{
          title: t('roles_overview.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}
        topActions={
          <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
            <Button bsStyle="info">{t('roles_overview.button_text')}</Button>
          </LinkContainer>
        }>
        <span>{t('roles_overview.description', { productName })}</span>
      </PageHeader>

      <Row className="content">
        <Col xs={12}>
          <Alert bsStyle="info" title={t('roles_overview.alert_title')}>
            {t('roles_overview.alert_body_prefix')}
            <b style={{ fontWeight: 'bold' }}>
              <Icon name="person_add" /> {t('roles_overview.share_text')}
            </b>{' '}
            {t('roles_overview.alert_body_suffix')}
            <DocumentationLink page={DocsHelper.PAGES.PERMISSIONS} text={t('roles_overview.documentation_text')} />.
          </Alert>
        </Col>
      </Row>

      <RolesOverview />
    </DocumentTitle>
  );
};

export default RolesOverviewPage;
