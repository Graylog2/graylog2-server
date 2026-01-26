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
import { useState, useMemo, useCallback } from 'react';
import { Button, Group } from '@mantine/core';

import { DocumentTitle, PageHeader } from 'components/common';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import { CollectorsPageNavigation } from 'components/collectors/common';
import { FleetFormModal } from 'components/collectors/fleets';
import { fetchPaginatedFleets, fleetsKeyFn } from 'components/collectors/hooks';
import type { Fleet } from 'components/collectors/types';
import customColumnRenderers from 'components/collectors/fleets/ColumnRenderers';
import { DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES } from 'components/collectors/fleets/Constants';

const CollectorsFleetsPage = () => {
  const [showFleetModal, setShowFleetModal] = useState(false);

  const columnRenderers = useMemo(() => customColumnRenderers(), []);

  const fetchEntities = useCallback(
    (searchParams: SearchParams) => fetchPaginatedFleets(searchParams),
    [],
  );

  const handleSaveFleet = (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => {
    // eslint-disable-next-line no-console
    console.log('Saving fleet:', fleet);
  };

  return (
    <DocumentTitle title="Collector Fleets">
      <CollectorsPageNavigation />
      <PageHeader
        title="Fleets"
        actions={(
          <Group>
            <Button onClick={() => setShowFleetModal(true)}>Add Fleet</Button>
          </Group>
        )}
      >
        <span>Manage collector fleets and their configurations.</span>
      </PageHeader>

      <PaginatedEntityTable<Fleet>
        humanName="fleets"
        tableLayout={DEFAULT_LAYOUT}
        additionalAttributes={ADDITIONAL_ATTRIBUTES}
        fetchEntities={fetchEntities}
        keyFn={fleetsKeyFn}
        entityAttributesAreCamelCase={false}
        columnRenderers={columnRenderers}
        entityActions={() => null}
      />

      {showFleetModal && (
        <FleetFormModal
          onClose={() => setShowFleetModal(false)}
          onSave={handleSaveFleet}
        />
      )}
    </DocumentTitle>
  );
};

export default CollectorsFleetsPage;
