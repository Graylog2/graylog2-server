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
import { useNavigate } from 'react-router-dom';

import Routes from 'routing/Routes';
import { Button, Row, Col } from 'components/bootstrap';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';
import LookupTableWizard from 'components/lookup-tables/lookup-table-form';

function LUTFormPage() {
  const navigate = useNavigate();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <LUTPageLayout
        documentTitle="Lookup Tables Create"
        pageTitle="Lookup Tables Create"
        pageDescription="Lookup tables can be used in extractors, converters and processing pipelines to translate message fields or to enrich messages."
        actions={
          <Button bsStyle="primary" onClick={() => navigate(Routes.SYSTEM.LOOKUPTABLES.OVERVIEW)}>
            Back to list
          </Button>
        }>
        <Row className="content" style={{ flexGrow: 1 }}>
          <Col md={12}>
            <LookupTableWizard />
          </Col>
        </Row>
      </LUTPageLayout>
    </div>
  );
}

export default LUTFormPage;
