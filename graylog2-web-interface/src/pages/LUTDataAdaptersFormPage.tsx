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
import { useParams } from 'react-router-dom';

import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { Spinner } from 'components/common';
import { Button, Row, Col } from 'components/bootstrap';
import { LUTPageLayout } from 'components/lookup-tables/layout-componets';
import { useFetchDataAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { DataAdapterFormView } from 'components/lookup-tables';
import { useTranslation } from 'react-i18next';

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
  const { push } = useHistory();
  const { t } = useTranslation('lookup_tables');
  const navigateBack = () => push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);

  return (
    <FlexContainer>
      <LUTPageLayout
        documentTitle={t('adapter_create_document_title')}
        pageTitle={t('adapter_create_page_title')}
        pageDescription={t('adapter_create_description')}
        actions={
          <Button bsStyle="primary" onClick={navigateBack}>
            {t('back_to_list')}
          </Button>
        }>
        <Row className="content" style={{ flexGrow: 1 }}>
          <FlexCol md={12}>
            {loadingDataAdapter ? (
              <Spinner text={t('loading_adapter')} />
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
