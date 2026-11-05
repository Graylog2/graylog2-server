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
import React from 'react';
import { useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader, IfPermitted } from 'components/common';
import { Alert, Row, Col } from 'components/bootstrap';
import IndexSetTemplatesList from 'components/indices/IndexSetTemplates/IndexSetTemplatesList';
import CreateIndexSetTemplateButton from 'components/indices/IndexSetTemplates/CreateIndexSetTemplateButton';
import { IndicesPageNavigation } from 'components/indices';

const IndexSetTemplatesPage = () => {
  const { t } = useTranslation('indexsets');

  return (
    <DocumentTitle title={t('pages.templates_title')}>
      <IndicesPageNavigation />
      <PageHeader
        title={t('pages.templates_title')}
        actions={
          <IfPermitted permissions="indexset_templates:create">
            <CreateIndexSetTemplateButton />
          </IfPermitted>
        }>
        <span>{t('pages.templates_description')}</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <Alert>{t('pages.templates_default_info')}</Alert>

          <IndexSetTemplatesList />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexSetTemplatesPage;
