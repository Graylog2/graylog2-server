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
import { useEffect, useRef } from 'react';

import { Row, Col, Alert } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Link } from 'components/common';
import PreviewBadge from 'components/common/PreviewBadge';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import { useFleet } from 'components/collectors/hooks/useFleetQueries';
import ConnectionSuccess from 'components/collectors/overview/onboarding/ConnectionSuccess';
import Routes from 'routing/Routes';
import useLocation from 'routing/useLocation';
import { extractErrorMessage } from 'util/extractErrorMessage';
import useFinishOnboarding from 'components/welcome/hooks/useFinishOnboarding';

const CollectorsOnboardingInstancePage = () => {
  const { instanceUid } = useParams<{ instanceUid: string }>();
  const location = useLocation<{ fleetName?: string } | null>();
  // Set by the onboarding wizard's history push; absent on direct visits.
  const stateFleetName = location.state?.fleetName;

  const { data: instance, isLoading, error } = useInstance(instanceUid);
  const { data: fleet } = useFleet(instance?.fleet_id ?? '');

  const { mutate: finish } = useFinishOnboarding();

  // Guards that the instance is actually there before we finish the onboarding. `useInstance`
  // polls on the heartbeat interval and returns a new object each time, so this must be latched
  // rather than keyed on the instance reference -- otherwise it re-POSTs every poll.
  // The COMPLETED telemetry event lives in `ConnectionSuccess`, which knows whether messages
  // are actually arriving.
  const finishedFor = useRef<string | null>(null);

  useEffect(() => {
    if (!instance || finishedFor.current === instance.instance_uid) return;

    finishedFor.current = instance.instance_uid;
    finish();
  }, [instance, finish]);

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

    return <ConnectionSuccess instance={instance} fleetName={fleet?.name ?? stateFleetName} />;
  };

  return (
    <DocumentTitle title="Collector Onboarding">
      <CollectorsPageNavigation />
      <PageHeader
        title={
          <>
            Collector Onboarding <PreviewBadge />
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
