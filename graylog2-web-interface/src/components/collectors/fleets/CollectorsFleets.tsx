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
import { useEffect, useState, useMemo, useCallback } from 'react';
import { useLocation } from 'react-router-dom';

import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

import { FleetFormModal } from './index';
import customColumnRenderers from './ColumnRenderers';
import { DEFAULT_LAYOUT } from './Constants';

import { fetchPaginatedFleets, fleetsKeyFn, useCollectorsMutations } from '../hooks';
import type { Fleet } from '../types';

const CollectorsFleets = () => {
  const [showFleetModal, setShowFleetModal] = useState(false);
  const { createFleet, isCreatingFleet } = useCollectorsMutations();
  const { pathname } = useLocation();
  const history = useHistory();

  useEffect(() => {
    setShowFleetModal(pathname === Routes.SYSTEM.COLLECTORS.FLEETS_NEW);
  }, [pathname]);

  const columnRenderers = useMemo(() => customColumnRenderers(), []);

  const fetchEntities = useCallback(
    (searchParams: SearchParams) => fetchPaginatedFleets(searchParams),
    [],
  );

  const closeCreateModal = useCallback(() => {
    history.push(Routes.SYSTEM.COLLECTORS.FLEETS);
  }, [history]);

  const handleSaveFleet = async (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => {
    await createFleet(fleet);
    closeCreateModal();
  };

  return (
    <>
      <PaginatedEntityTable<Fleet>
        humanName="fleets"
        tableLayout={DEFAULT_LAYOUT}
        fetchEntities={fetchEntities}
        keyFn={fleetsKeyFn}
        entityAttributesAreCamelCase={false}
        columnRenderers={columnRenderers}
        entityActions={() => null}
      />

      {showFleetModal && (
        <FleetFormModal
          onClose={closeCreateModal}
          onSave={handleSaveFleet}
          isLoading={isCreatingFleet}
        />
      )}
    </>
  );
};

export default CollectorsFleets;
