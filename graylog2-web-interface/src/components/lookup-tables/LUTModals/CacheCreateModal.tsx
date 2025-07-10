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

import connect from 'stores/connect';
import { Modal } from 'components/bootstrap';
import { CacheCreate } from 'components/lookup-tables';
import { LookupTableCachesActions, LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';

type Props = {
  onClose: () => void,
}

const CacheCreateModal = ({ onClose }: Props) => {
  const validateCache = (adapter) => {
    LookupTableCachesActions.validate(adapter);
  };

  return (
    <Modal show fullScreen onHide={onClose} transitionProps={{ transition: 'fade', duration: 500 }}>
      <Modal.Header>
        <Modal.Title>Create Cache</Modal.Title>
      </Modal.Header>
      <CacheCreate
        saved={onClose}
        validate={validateCache}
        onCancel={onClose}
      />
    </Modal>
  );
}

export default connect(
  CacheCreateModal,
  { cachesStore: LookupTableCachesStore },
  ({ cachesStore, ...otherProps }) => ({
    ...otherProps,
    ...cachesStore,
  }),
);
