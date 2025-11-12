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
import { useFormikContext } from 'formik';

import { Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import { useFetchDataAdapter, useFetchAllDataAdapters } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { LookupTable, LookupTableAdapter } from 'logic/lookup-tables/types';

import DataAdapterPicker from './AdapterPicker';
import DataAdapterFormView from './AdapterFormView';

const FlexCol = styled(Col)`
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 2rem;
`;

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

function AdapterReadOnly({ dataAdapter }: { dataAdapter: LookupTableAdapter }) {
  const plugins = usePluginEntities('lookupTableAdapters');
  const adapterPlugin = React.useMemo(
    () => plugins.find((p: any) => p.type === dataAdapter?.config?.type),
    [dataAdapter?.config?.type, plugins],
  );
  const DocComponent = React.useMemo(() => adapterPlugin.documentationComponent, [adapterPlugin]);

  return (
    <StyledRow>
      <Col lg={9}>
        <Col lg={6}>
          <DataAdapter dataAdapter={dataAdapter} noEdit />
        </Col>
        <Col lg={6}>{DocComponent ? <DocComponent dataAdapterId={dataAdapter?.id} /> : null}</Col>
      </Col>
    </StyledRow>
  );
}

function DataAdapterFormStep() {
  const { values, setFieldValue } = useFormikContext<LookupTable>();
  const { allDataAdapters, loadingAllDataAdapters } = useFetchAllDataAdapters();
  const { dataAdapter, loadingDataAdapter } = useFetchDataAdapter(values.data_adapter_id);
  const [showForm, setShowForm] = React.useState<boolean>(false);
  const showAdapter = React.useMemo(() => values.data_adapter_id, [values.data_adapter_id]);

  const onSaved = (newDataAdapter: LookupTableAdapter) => {
    setFieldValue('data_adapter_id', newDataAdapter.id);
    setShowForm(false);
  };

  const onCancel = () => {
    setFieldValue('data_adapter_id', '');
    setShowForm(false);
  };

  const onCreateClick = () => {
    onCancel();
    setTimeout(() => setShowForm(true), 100);
  };

  return (
    <Row className="content" style={{ flexGrow: 1 }}>
      <FlexCol md={12}>
        {loadingAllDataAdapters ? (
          <Spinner text="Loading data adapters..." />
        ) : (
          <>
            <StyledRow>
              <Col lg={6}>
                <DataAdapterPicker onCreateClick={onCreateClick} dataAdapters={allDataAdapters} />
              </Col>
            </StyledRow>
            {showAdapter && !loadingDataAdapter && <AdapterReadOnly dataAdapter={dataAdapter} />}
            {showForm && !showAdapter && <DataAdapterFormView onCancel={onCancel} saved={onSaved} />}
          </>
        )}
      </FlexCol>
    </Row>
  );
}

export default DataAdapterFormStep;
