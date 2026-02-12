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
import { Navigate } from 'react-router-dom';

import { Button, Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { FleetFormModal } from 'components/collectors/fleets';
import { fetchPaginatedFleets, fleetsKeyFn, useCollectorsMutations } from 'components/collectors/hooks';
import { useCollectorsConfig } from 'components/collectors/hooks/useCollectors';
import type { Fleet } from 'components/collectors/types';
import customColumnRenderers from 'components/collectors/fleets/ColumnRenderers';
import { DEFAULT_LAYOUT } from 'components/collectors/fleets/Constants';
import Routes from 'routing/Routes';

const CollectorsFleetsPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();
  const [showFleetModal, setShowFleetModal] = useState(false);
  const { createFleet, isCreatingFleet } = useCollectorsMutations();

  const columnRenderers = useMemo(() => customColumnRenderers(), []);

  const fetchEntities = useCallback(
    (searchParams: SearchParams) => fetchPaginatedFleets(searchParams),
    [],
  );

  const handleSaveFleet = async (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => {
    await createFleet(fleet);
    setShowFleetModal(false);
  };

  if (isLoading) {
    return <Spinner />;
  }

  if (!config?.opamp_ca_id) {
    return <Navigate to={Routes.SYSTEM.COLLECTORS.SETTINGS} />;
  }

  return (
    <DocumentTitle title="Collector Fleets">
      <CollectorsPageNavigation />
      <PageHeader
        title="Fleets"
        actions={<Button bsStyle="success" onClick={() => setShowFleetModal(true)}>Add Fleet</Button>}
      >
        <span>Manage collector fleets and their configurations.</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <PaginatedEntityTable<Fleet>
            humanName="fleets"
            tableLayout={DEFAULT_LAYOUT}
            fetchEntities={fetchEntities}
            keyFn={fleetsKeyFn}
            entityAttributesAreCamelCase={false}
            columnRenderers={columnRenderers}
            entityActions={() => null}
          />
        </Col>
      </Row>

      {showFleetModal && (
        <FleetFormModal
          onClose={() => setShowFleetModal(false)}
          onSave={handleSaveFleet}
          isLoading={isCreatingFleet}
        />
      )}
    </DocumentTitle>
  );
};

export default CollectorsFleetsPage;
