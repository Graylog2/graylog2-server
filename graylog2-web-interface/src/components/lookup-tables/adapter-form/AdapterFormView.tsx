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

import { RowContainer } from 'components/lookup-tables/layout-componets';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

import DataAdapterForm from './AdapterForm';
import DataAdapterTypeSelect from './AdapterTypeSelect';

type Props = {
  saved?: (adapterObj: LookupTableAdapter) => void;
  onCancel: () => void;
  adapter?: LookupTableAdapter;
};

const DataAdapterFormView = ({ saved = undefined, onCancel, adapter = undefined }: Props) => {
  const [dataAdapter, setDataAdapter] = React.useState<LookupTableAdapter>(adapter);
  const isCreate = React.useMemo(() => !dataAdapter?.id, [dataAdapter]);

  return (
    <>
      {isCreate && (
        <RowContainer>
          <DataAdapterTypeSelect
            adapterConfigType={dataAdapter ? dataAdapter.config.type : null}
            onAdapterChange={setDataAdapter}
          />
        </RowContainer>
      )}
      {dataAdapter && (
        <DataAdapterForm
          dataAdapter={dataAdapter}
          type={dataAdapter?.config?.type}
          create={isCreate}
          title="Configure Adapter"
          saved={saved}
          onCancel={onCancel}
        />
      )}
    </>
  );
};

export default DataAdapterFormView;
