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

import { DocumentTitle, PageHeader } from 'components/common';
import { Alert, Row, Col } from 'components/bootstrap';
import IndexSetTemplatesList from 'components/indices/IndexSetTemplates/IndexSetTemplatesList';
import CreateIndexSetTemplateButton from 'components/indices/IndexSetTemplates/CreateIndexSetTemplateButton';
import { IndicesPageNavigation } from 'components/indices';

const IndexSetTemplatesPage = () => (
  <DocumentTitle title="Index Set Templates">
    <IndicesPageNavigation />
    <PageHeader title="Index Set Templates"
                actions={<CreateIndexSetTemplateButton />}>
      <span>
        View and manage your Index Set Templates. These allow Index Set configurations to be saved and re-used upon creating a new Index Sets.
      </span>
    </PageHeader>

    <Row className="content">
      <Col md={12}>
        <Alert>Define a default template here in order to control the configuration of Index Sets created by Illuminate packs.</Alert>

        <IndexSetTemplatesList />
      </Col>
    </Row>
  </DocumentTitle>
);

export default IndexSetTemplatesPage;
