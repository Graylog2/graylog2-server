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

<<<<<<< HEAD
import LUTdrawer from './LUTDrawer';

export type ModalTypes = 'LUT' | 'CACHE' | 'DATA-ADAPTER';
=======
import { useModalContext } from './ModalContext';
import CacheCreateModal from './CacheCreateModal';
import DataAdapterCreateModal from './DataAdapterCreateModal';
import CacheEditModal from './CacheEditModal';
import DataAdapterEditModal from './DataAdapterEditModal';

export type ModalTypes =
  | 'LUT'
  | 'CACHE'
  | 'DATA-ADAPTER'
  | 'CACHE-CREATE'
  | 'DATA-ADAPTER-CREATE'
  | 'CACHE-EDIT'
  | 'DATA-ADAPTER-EDIT';
>>>>>>> 9fb7c4a4c7cc6b444f9425afdda493d48596ec73

function LUTModals() {
  const { modal, setModal, entity, setEntity, title, setTitle } = useModalContext();

  const onClose = () => {
    setModal(null);
    setTitle(null);
    setEntity(null);
  };

  switch (modal) {
    case 'LUT':
      return (
        <LUTdrawer title={title} onClose={onClose}>
          {entity}
        </LUTdrawer>
      );
    case 'CACHE':
      return (
        <LUTdrawer title={title} onClose={onClose}>
          {entity}
        </LUTdrawer>
      );
    case 'DATA-ADAPTER':
<<<<<<< HEAD
      return (
        <LUTdrawer title={title} onClose={onClose}>
          {entity}
        </LUTdrawer>
      );
=======
      return <LUTdrawer title={title} onClose={onClose}>{entity}</LUTdrawer>;
    case 'CACHE-CREATE':
      return <CacheCreateModal onClose={onClose} />;
    case 'DATA-ADAPTER-CREATE':
      return <DataAdapterCreateModal onClose={onClose} />;
    case 'CACHE-EDIT':
      return <CacheEditModal onClose={onClose} title={title} cache={entity} />;
    case 'DATA-ADAPTER-EDIT':
      return <DataAdapterEditModal onClose={onClose} title={title} dataAdapter={entity} />;
>>>>>>> 9fb7c4a4c7cc6b444f9425afdda493d48596ec73
    default:
      return null;
  }
}

export default LUTModals;
