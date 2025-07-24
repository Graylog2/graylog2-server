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

import { useModalContext } from 'components/lookup-tables/contexts/ModalContext';

import LUTdrawer from './LUTDrawer';
import CacheCreateModal from './CacheCreateModal';
import DataAdapterCreateModal from './DataAdapterCreateModal';
import CacheEditModal from './CacheEditModal';
import DataAdapterEditModal from './DataAdapterEditModal';
import LUTCreateModal from './LUTCreateModal';

export type ModalTypes =
  | 'LUT'
  | 'CACHE'
  | 'DATA-ADAPTER'
  | 'LUT-CREATE'
  | 'LUT-EDIT'
  | 'CACHE-CREATE'
  | 'DATA-ADAPTER-CREATE'
  | 'CACHE-EDIT'
  | 'DATA-ADAPTER-EDIT';

function LUTModals() {
  const { modal, setModal, entity, setEntity, title, setTitle } = useModalContext();

  const onClose = () => {
    setModal(null);
    setTitle(null);
    setEntity(null);
  };

  switch (modal) {
    case 'LUT':
    case 'CACHE':
    case 'DATA-ADAPTER':
      return (
        <LUTdrawer title={title} onClose={onClose}>
          {entity}
        </LUTdrawer>
      );
    case 'LUT-CREATE':
      return <LUTCreateModal onClose={onClose} />;
    case 'LUT-EDIT':
      return <LUTCreateModal onClose={onClose} title={title} lut={entity} />;
    case 'CACHE-CREATE':
      return <CacheCreateModal onClose={onClose} />;
    case 'DATA-ADAPTER-CREATE':
      return <DataAdapterCreateModal onClose={onClose} />;
    case 'CACHE-EDIT':
      return <CacheEditModal onClose={onClose} title={title} cache={entity} />;
    case 'DATA-ADAPTER-EDIT':
      return <DataAdapterEditModal onClose={onClose} title={title} dataAdapter={entity} />;
    default:
      return null;
  }
}

export default LUTModals;
