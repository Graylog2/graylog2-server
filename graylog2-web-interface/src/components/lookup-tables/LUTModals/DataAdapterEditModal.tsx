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

import connect from 'stores/connect';
import { Row, Col, Modal } from 'components/bootstrap';
import { DataAdapterForm } from 'components/lookup-tables';
import { LookupTableDataAdaptersStore } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import { useValidateDataAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

type Props = {
  onClose: () => void;
  dataAdapter: LookupTableAdapter;
  title: string;
};

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

const DataAdapterEditModal = ({ onClose, title, dataAdapter, validationErrors }: Props & { validationErrors: any }) => {
  const { validateDataAdapter } = useValidateDataAdapter();

  const validate = (dataAdapterObj: LookupTableAdapter) => {
    validateDataAdapter(dataAdapterObj);
  };

  return (
    <Modal show fullScreen onHide={onClose}>
      <Modal.Header>
        <Modal.Title>{`Edit ${title}`}</Modal.Title>
      </Modal.Header>
      <StyledRow>
        <Col lg={9}>
          <DataAdapterForm
            type={dataAdapter.config.type}
            saved={onClose}
            onCancel={onClose}
            title={title}
            validate={validate}
            validationErrors={validationErrors}
            create={false}
            dataAdapter={dataAdapter}
          />
        </Col>
      </StyledRow>
    </Modal>
  );
};

export default connect(
  DataAdapterEditModal,
  { dataAdaptersStore: LookupTableDataAdaptersStore },
  ({ dataAdaptersStore, ...otherProps }) => ({
    ...otherProps,
    ...dataAdaptersStore,
  }),
);
