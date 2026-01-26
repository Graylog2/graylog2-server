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
import { useState } from 'react';
import { Button, Group } from '@mantine/core';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { FleetList, FleetFormModal } from 'components/collectors/fleets';
import { useFleets } from 'components/collectors/hooks';
import { CollectorsPageNavigation } from 'components/collectors/common';
import type { Fleet } from 'components/collectors/types';

const CollectorsFleetsPage = () => {
  const { data: fleets, isLoading } = useFleets();
  const [showFleetModal, setShowFleetModal] = useState(false);

  const handleSaveFleet = (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => {
    // Mock save - in real implementation this would call an API
    // eslint-disable-next-line no-console
    console.log('Saving fleet:', fleet);
  };

  return (
    <DocumentTitle title="Collector Fleets">
      <CollectorsPageNavigation />
      <PageHeader title="Fleets"
        actions={(
          <Group>
            <Button onClick={() => setShowFleetModal(true)}>Add Fleet</Button>
          </Group>
        )}>
        <span>Manage collector fleets and their configurations.</span>
      </PageHeader>
      {isLoading ? <Spinner /> : <FleetList fleets={fleets || []} />}

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
