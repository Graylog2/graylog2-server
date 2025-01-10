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
import React, { useCallback, useEffect, useState } from 'react';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import { CollectorsActions } from 'stores/sidecars/CollectorsStore';
import CollectorForm from 'components/sidecars/configuration-forms/CollectorForm';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import DocsHelper from 'util/DocsHelper';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';
import type { Collector } from 'components/sidecars/types';

const SidecarEditCollectorPage = () => {
  const history = useHistory();
  const { collectorId } = useParams();
  const [collector, setCollector] = useState<Collector>();

  const _reloadCollector = useCallback(() => {
    CollectorsActions.getCollector(collectorId).then(
      (result) => setCollector(result),
      (error) => {
        if (error.status === 404) {
          history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION);
        }
      },
    );
  }, [collectorId, history]);

  useEffect(() => {
    _reloadCollector();
  }, [_reloadCollector]);

  if (!collector) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Log Collector">
      <SidecarsPageNavigation />
      <PageHeader title="Log Collector"
                  documentationLink={{
                    title: 'Sidecar documentation',
                    path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                  }}>
        <span>
          Some words about log collectors.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={6}>
          <CollectorForm action="edit" collector={collector} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default SidecarEditCollectorPage;
