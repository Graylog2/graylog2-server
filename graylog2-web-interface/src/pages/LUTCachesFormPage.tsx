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
import { useNavigate, useParams } from 'react-router-dom';

import Routes from 'routing/Routes';
import { Spinner } from 'components/common';
import { Button, Row, Col } from 'components/bootstrap';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';
import { useFetchCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { CacheFormView } from 'components/lookup-tables';

const FlexContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const FlexCol = styled(Col)`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

function LUTDataAdaptersFormPage() {
  const { cacheIdOrName } = useParams<{ cacheIdOrName: string }>();
  const { cache, loadingCache } = useFetchCache(cacheIdOrName);
  const navigate = useNavigate();
  const navigateBack = () => navigate(Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW);

  return (
    <FlexContainer>
      <LUTPageLayout
        documentTitle="Lookup Tables - Caches Create"
        pageTitle="Create Caches for Lookup Tables"
        pageDescription="Caches provide the actual values for lookup tables."
        actions={
          <Button bsStyle="primary" onClick={navigateBack}>
            Back to list
          </Button>
        }>
        <Row className="content" style={{ flexGrow: 1 }}>
          <FlexCol md={12}>
            {loadingCache ? (
              <Spinner text="Loading Cache" />
            ) : (
              <CacheFormView onCancel={navigateBack} saved={navigateBack} cache={cache} />
            )}
          </FlexCol>
        </Row>
      </LUTPageLayout>
    </FlexContainer>
  );
}

export default LUTDataAdaptersFormPage;
