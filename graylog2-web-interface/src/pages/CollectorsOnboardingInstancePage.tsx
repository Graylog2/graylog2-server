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
import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

import { Row, Col, Alert } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Link } from 'components/common';
import BetaBadge from 'components/common/BetaBadge';
import { CollectorsPageNavigation } from 'components/collectors/common';
import collectorReceivedMessagesUrl from 'components/collectors/common/collectorReceivedMessagesUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from 'components/collectors/common/fields';
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import Routes from 'routing/Routes';
import { extractErrorMessage } from 'util/extractErrorMessage';
import useDefaultInterval from 'views/hooks/useDefaultIntervalForRefresh';

const CollectorsOnboardingInstancePage = () => {
  const { instanceUid } = useParams<{ instanceUid: string }>();
  const navigate = useNavigate();

  const { data: instance, isLoading, error } = useInstance(instanceUid);
  const defaultInterval = useDefaultInterval();

  // using useEffect to guard that the default is actually there when we call the navigate
  useEffect(() => {
    if (instance && defaultInterval) {
      navigate(collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid, defaultInterval));
    }
  }, [defaultInterval, instance, navigate]);

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

    return <Spinner />;
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
