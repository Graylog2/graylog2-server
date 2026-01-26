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

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { InstanceList } from 'components/collectors/instances';
import { useInstances, useFleets, useSources } from 'components/collectors/hooks';
import { CollectorsPageNavigation } from 'components/collectors/common';

const CollectorsInstancesPage = () => {
  const { data: instances, isLoading: instancesLoading } = useInstances();
  const { data: fleets, isLoading: fleetsLoading } = useFleets();
  const { data: sources, isLoading: sourcesLoading } = useSources();

  const isLoading = instancesLoading || fleetsLoading || sourcesLoading;

  const fleetNames = (fleets || []).reduce(
    (acc, fleet) => ({ ...acc, [fleet.id]: fleet.name }),
    {} as Record<string, string>,
  );

  return (
    <DocumentTitle title="Collector Instances">
      <CollectorsPageNavigation />
      <PageHeader title="Instances">
        <span>View all collector instances across fleets.</span>
      </PageHeader>
      {isLoading ? (
        <Spinner />
      ) : (
        <InstanceList instances={instances || []} fleetNames={fleetNames} sources={sources || []} />
      )}
    </DocumentTitle>
  );
};

export default CollectorsInstancesPage;
