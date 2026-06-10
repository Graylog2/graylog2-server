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
import { useParams } from 'react-router-dom';

import { Row, Col, Alert } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Link } from 'components/common';
import BetaBadge from 'components/common/BetaBadge';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import { useFleet } from 'components/collectors/hooks/useFleetQueries';
import ConnectionSuccess from 'components/collectors/overview/onboarding/ConnectionSuccess';
import type { PlatformId } from 'components/collectors/overview/onboarding/platforms';
import Routes from 'routing/Routes';
import useLocation from 'routing/useLocation';
import { extractErrorMessage } from 'util/extractErrorMessage';

const CollectorsOnboardingInstancePage = () => {
  const { instanceUid } = useParams<{ instanceUid: string }>();
  const location = useLocation<{ platformId?: PlatformId; fleetName?: string } | null>();
  // Set by the onboarding wizard's history push; absent on direct visits.
  const platformId = location.state?.platformId;
  const stateFleetName = location.state?.fleetName;

  const { data: instance, isLoading, error } = useInstance(instanceUid);
  const { data: fleet } = useFleet(instance?.fleet_id ?? '');

  const content = () => {
    if (isLoading) return <Spinner />;

    if (error) {
      return <Alert bsStyle="danger">Could not load collector instance: {extractErrorMessage(error)}</Alert>;
    }

    if (!instance) {
      return (
        <Alert bsStyle="warning">
          Collector instance not found. It may have been removed &mdash; see all{' '}
          <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES}>Instances</Link>.
        </Alert>
      );
    }

    return <ConnectionSuccess platformId={platformId} instance={instance} fleetName={fleet?.name ?? stateFleetName} />;
  };

  return (
    <DocumentTitle title="Collector Onboarding">
      <CollectorsPageNavigation />
      <PageHeader
        title={
          <>
            Collector Onboarding <BetaBadge />
          </>
        }>
        <span>Status of your newly connected collector.</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>{content()}</Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsOnboardingInstancePage;
