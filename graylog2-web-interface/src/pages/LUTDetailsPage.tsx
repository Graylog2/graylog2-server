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
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';

import { Button, Row, Col } from 'components/bootstrap';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';
import { LookupTableView } from 'components/lookup-tables';

const FlexContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const FlexCol = styled(Col)`
  display: flex;
  flex-direction: column;
  height: 100%;

  & .flex-row {
    flex-grow: 1;
  }

  & .content.row {
    box-shadow: none;
  }
`;

function LUTDetailsPage() {
  const navigate = useNavigate();

  return (
    <FlexContainer>
      <LUTPageLayout
        documentTitle="Lookup Table Details"
        pageTitle="Lookup Tables Details"
        pageDescription="Lookup tables can be used in extractors, converters and processing pipelines to translate message fields or to enrich messages."
        actions={
          <Button bsStyle="primary" onClick={() => navigate(-1)}>
            Back to list
          </Button>
        }>
        <Row className="content" style={{ flexGrow: 1 }}>
          <FlexCol md={12}>
            <LookupTableView />
          </FlexCol>
        </Row>
      </LUTPageLayout>
    </FlexContainer>
  );
}

export default LUTDetailsPage;
