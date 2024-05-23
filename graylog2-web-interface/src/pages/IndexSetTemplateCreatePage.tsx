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
import { Col, Row } from 'components/bootstrap';
import { IndicesPageNavigation } from 'components/indices';
import CreateTemplate from 'components/indices/IndexSetTemplates/CreateTemplate';

const IndexSetTemplateCreatePage = () => (
  <DocumentTitle title="Create Index Set Template">
    <IndicesPageNavigation />
    <PageHeader title="Create Index Set Template">
      <span>Create a new Index Set Template. This is a set of configuration that can be applied when creating a new Index Set.</span>
    </PageHeader>
    <Row className="content">
      <Col md={12}>
        <CreateTemplate />
      </Col>
    </Row>
  </DocumentTitle>
);

export default IndexSetTemplateCreatePage;
