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

import { Col, Row, ButtonToolbar } from 'components/bootstrap';
import HideOnCloud from 'util/conditional/HideOnCloud';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader, IfPermitted } from 'components/common';
import { IndicesPageNavigation } from 'components/indices';
import IndexSetsOverview from 'components/indices/IndexSetsOverview';
import { IndexerClusterHealth } from 'components/indexers';
import AllIndicesMaintenanceDropdown from 'components/indices/AllIndicesMaintenanceDropdown';
import CreateButton from 'components/common/CreateButton';

const IndicesPage = () => {
  const { t } = useTranslation('indexsets');

  return (
    <DocumentTitle title={t('pages.indices_document_title')}>
      <IndicesPageNavigation />
      <PageHeader
        title={t('pages.indices_title')}
        actions={
          <ButtonToolbar>
            <CreateButton entityKey="Index Set" />
            <IfPermitted permissions="indexranges:rebuild">
              <AllIndicesMaintenanceDropdown />
            </IfPermitted>
          </ButtonToolbar>
        }
        documentationLink={{
          title: t('overview.header.documentation_link'),
          path: DocsHelper.PAGES.INDEX_MODEL,
        }}>
        <span>{t('pages.indices_description')}</span>
      </PageHeader>

      <HideOnCloud>
        <IndexerClusterHealth minimal />
      </HideOnCloud>

      <Row className="content">
        <Col md={12}>
          <IndexSetsOverview />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndicesPage;
