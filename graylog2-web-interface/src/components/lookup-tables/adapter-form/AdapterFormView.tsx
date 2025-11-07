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

import { Row, Col } from 'components/bootstrap';
import { DataAdapterForm, DataAdapterTypeSelect } from 'components/lookup-tables';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

const FlexCol = styled(Col)`
  display: flex;
  flex-direction: column;
`;

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
        <StyledRow>
          <Col lg={6}>
            <DataAdapterTypeSelect
              adapterConfigType={dataAdapter ? dataAdapter.config.type : null}
              onAdapterChange={setDataAdapter}
            />
          </Col>
        </StyledRow>
      )}
      {dataAdapter && (
        <StyledRow style={{ flexGrow: 1 }}>
          <FlexCol lg={9}>
            <DataAdapterForm
              dataAdapter={dataAdapter}
              type={dataAdapter?.config?.type}
              create={isCreate}
              title="Configure Adapter"
              saved={saved}
              onCancel={onCancel}
            />
          </FlexCol>
        </StyledRow>
      )}
    </>
  );
};

export default DataAdapterFormView;
