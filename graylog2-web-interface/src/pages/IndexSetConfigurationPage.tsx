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

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm, IndicesPageNavigation } from 'components/indices';
import { useStore } from 'stores/connect';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import useParams from 'routing/useParams';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import useQuery from 'routing/useQuery';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const _saveConfiguration = (history: HistoryFunction, indexSet: IndexSet) => IndexSetsActions.update(indexSet).then(() => {
  history.push(Routes.SYSTEM.INDICES.LIST);
});

const IndexSetConfigurationPage = () => {
  const { indexSetId } = useParams();
  const { indexSet } = useStore(IndexSetsStore);
  const {
    retentionStrategies,
    rotationStrategies,
    retentionStrategiesContext,
  } = useStore(IndicesConfigurationStore);
  const history = useHistory();
  const { from } = useQuery();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    IndexSetsActions.get(indexSetId);
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  }, [indexSetId]);

  const formCancelLink = () => {
    if (from === 'details') {
      return Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id);
    }

    return Routes.SYSTEM.INDICES.LIST;
  };

  const isLoading = () => !indexSet || !rotationStrategies || !retentionStrategies || (indexSetId !== indexSet.id);

  if (isLoading()) {
    return <Spinner />;
  }

  const saveConfiguration = (newIndexSet: IndexSet) => {
    _saveConfiguration(history, newIndexSet);

    sendTelemetry(TELEMETRY_EVENT_TYPE.INDICES.INDEX_SET_UPDATED, {
      app_pathname: 'indexsets',
      app_section: 'indexset',
    });
  };

  return (
    <DocumentTitle title="Configure Index Set">
      <IndicesPageNavigation />
      <div>
        <PageHeader title="Configure Index Set"
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}>
          <span>
            Modify the current configuration for this index set, allowing you to customize the retention, sharding,
            and replication of messages coming from one or more streams.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IndexSetConfigurationForm indexSet={indexSet}
                                       retentionStrategiesContext={retentionStrategiesContext}
                                       rotationStrategies={rotationStrategies}
                                       retentionStrategies={retentionStrategies}
                                       submitButtonText="Update index set"
                                       submitLoadingText="Updating index set..."
                                       cancelLink={formCancelLink()}
                                       onUpdate={saveConfiguration} />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default IndexSetConfigurationPage;
