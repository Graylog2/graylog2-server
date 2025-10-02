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
import { CacheForm } from 'components/lookup-tables';
import { LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';
import { useValidateCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableCache } from 'logic/lookup-tables/types';

type Props = {
  onClose: () => void;
  cache: LookupTableCache;
  title: string;
};

const StyledRow = styled(Row)`
  display: flex;
  width: 100%;
  justify-content: center;
`;

const CacheEditModal = ({ onClose, title, cache, validationErrors }: Props & { validationErrors: any }) => {
  const { validateCache } = useValidateCache();

  const validate = (cacheObj: LookupTableCache) => {
    validateCache(cacheObj);
  };

  return (
    <Modal show fullScreen onHide={onClose}>
      <Modal.Header>
        <Modal.Title>{`Edit ${title}`}</Modal.Title>
      </Modal.Header>
      <StyledRow>
        <Col lg={9}>
          <CacheForm
            type={cache.config.type}
            saved={onClose}
            onCancel={onClose}
            title={title}
            validate={validate}
            validationErrors={validationErrors}
            create={false}
            cache={cache}
          />
        </Col>
      </StyledRow>
    </Modal>
  );
};

export default connect(CacheEditModal, { cachesStore: LookupTableCachesStore }, ({ cachesStore, ...otherProps }) => ({
  ...otherProps,
  ...cachesStore,
}));
