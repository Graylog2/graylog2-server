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

import { Row, Col, Tabs, Badge } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import PreviewBadge from 'components/common/PreviewBadge';
import { DeployTab, EnrollmentTokenList } from 'components/collectors/deployment';
import { CollectorsPageNavigation } from 'components/collectors/common';
import { useCollectorsConfig, useEnrollmentTokenCount } from 'components/collectors/hooks';
import Routes from 'routing/Routes';

const CollectorsDeploymentPage = () => {
  const { data: config, isLoading } = useCollectorsConfig();
  const tokenCount = useEnrollmentTokenCount();

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
            Deploy Collectors <PreviewBadge />
          </>
        }>
        <span>
          Run the command on any number of hosts &mdash; they enroll into the fleet you pick and appear as they check
          in.
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <Tabs defaultValue="deploy">
            <Tabs.List>
              <Tabs.Tab value="deploy">Deploy</Tabs.Tab>
              <Tabs.Tab value="tokens">
                Enrollment tokens{tokenCount !== undefined && <> <Badge>{tokenCount}</Badge></>}
              </Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="deploy">
              <DeployTab />
            </Tabs.Panel>
            <Tabs.Panel value="tokens">
              <p className="description">
                Tokens authorize new Collectors to enroll into a fleet. Deleting a token does not affect
                already-enrolled Collectors.
              </p>
              <EnrollmentTokenList />
            </Tabs.Panel>
          </Tabs>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CollectorsDeploymentPage;
