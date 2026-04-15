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
import { Navigate } from 'react-router-dom';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import BetaBadge from 'components/common/BetaBadge';
import { DeploymentForm, EnrollmentTokenList } from 'components/collectors/deployment';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useCollectorsConfig } from 'components/collectors/hooks';
import Routes from 'routing/Routes';

const CollectorsDeploymentPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();

  if (isLoading) {
    return <Spinner />;
  }

  if (!config?.signing_cert_id) {
    return <Navigate to={Routes.SYSTEM.COLLECTORS.SETTINGS} />;
  }

  return (
    <DocumentTitle title="Deploy Collectors">
      <CollectorsPageNavigation />
      <PageHeader
        title={
          <>
            Deploy Collectors <BetaBadge />
          </>
        }>
        <span>
          Deploy collectors to your infrastructure using enrollment tokens. An enrollment token authorizes
          a collector to join a specific fleet and establishes a secure connection using mutual TLS.
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <DeploymentForm />
        </Col>
      </Row>
      <Row className="content">
        <Col md={12}>
          <h2>Enrollment Tokens</h2>
          <p>
            Tokens authorize new collectors to enroll into a fleet. Deleting a token does not affect
            already-enrolled collectors.
          </p>
          <EnrollmentTokenList />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsDeploymentPage;
