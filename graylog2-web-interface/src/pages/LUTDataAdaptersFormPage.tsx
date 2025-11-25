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
import { useFetchDataAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { DataAdapterFormView } from 'components/lookup-tables';

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
  const { adapterIdOrName } = useParams<{ adapterIdOrName: string }>();
  const { dataAdapter, loadingDataAdapter } = useFetchDataAdapter(adapterIdOrName);
  const navigate = useNavigate();
  const navigateBack = () => navigate(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);

  return (
    <FlexContainer>
      <LUTPageLayout
        documentTitle="Lookup Tables - Data Adapters Create"
        pageTitle="Create Data Adapters for Lookup Tables"
        pageDescription="Data adapters provide the actual values for lookup tables."
        actions={
          <Button bsStyle="primary" onClick={navigateBack}>
            Back to list
          </Button>
        }>
        <Row className="content" style={{ flexGrow: 1 }}>
          <FlexCol md={12}>
            {loadingDataAdapter ? (
              <Spinner text="Loading Data Adapter" />
            ) : (
              <DataAdapterFormView onCancel={navigateBack} saved={navigateBack} adapter={dataAdapter} />
            )}
          </FlexCol>
        </Row>
      </LUTPageLayout>
    </FlexContainer>
  );
}

export default LUTDataAdaptersFormPage;
