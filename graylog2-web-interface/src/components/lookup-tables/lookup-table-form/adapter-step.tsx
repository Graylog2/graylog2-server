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
import { useMemo, useState } from 'react';
import { useFormikContext } from 'formik';

import { Spinner } from 'components/common';
import { RowContainer, ColContainer } from 'components/lookup-tables/layout-componets';
import useScopePermissions from 'hooks/useScopePermissions';
import { useFetchDataAdapter, useFetchAllDataAdapters } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import DataAdapterPicker from 'components/lookup-tables/adapter-form/AdapterPicker';
import DataAdapterFormView from 'components/lookup-tables/adapter-form/AdapterFormView';
import type { LookupTable, LookupTableAdapter } from 'logic/lookup-tables/types';
import AdapterShow from 'components/lookup-tables/adapter-view/adapter-show';

function DataAdapterFormStep() {
  const { values, setFieldValue } = useFormikContext<LookupTable>();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(values);
  const { allDataAdapters, loadingAllDataAdapters } = useFetchAllDataAdapters();
  const { dataAdapter, loadingDataAdapter } = useFetchDataAdapter(values.data_adapter_id);
  const [showForm, setShowForm] = useState<boolean>(false);
  const showAdapter = useMemo(() => !!dataAdapter, [dataAdapter]);

  const canModify = useMemo(
    () => !values.id || (!loadingScopePermissions && scopePermissions?.is_mutable),
    [values.id, loadingScopePermissions, scopePermissions?.is_mutable],
  );

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
    <ColContainer $gap="lg" $align="center">
      {loadingAllDataAdapters ? (
        <RowContainer>
          <Spinner text="Loading data adapters..." />
        </RowContainer>
      ) : (
        <>
          {canModify && !showForm && (
            <RowContainer>
              <DataAdapterPicker onCreateClick={onCreateClick} dataAdapters={allDataAdapters} />
            </RowContainer>
          )}
          {showAdapter && !loadingDataAdapter && <AdapterShow dataAdapter={dataAdapter} />}
          {showForm && !showAdapter && <DataAdapterFormView onCancel={onCancel} saved={onSaved} isStep />}
        </>
      )}
    </ColContainer>
  );
}

export default DataAdapterFormStep;
