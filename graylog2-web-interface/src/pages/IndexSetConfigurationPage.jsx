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

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { useStore } from 'stores/connect';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';

import useSendTelemetry from '../logic/telemetry/useSendTelemetry';

const _saveConfiguration = (history, indexSet) => IndexSetsActions.update(indexSet).then(() => {
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
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    IndexSetsActions.get(indexSetId);
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  }, [indexSetId]);

  const formCancelLink = () => {
    if (history?.query?.from === 'details') {
      return Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id);
    }

    return Routes.SYSTEM.INDICES.LIST;
  };

  const isLoading = () => !indexSet || !rotationStrategies || !retentionStrategies || (indexSetId !== indexSet.id);

  if (isLoading()) {
    return <Spinner />;
  }

  const saveConfiguration = (newIndexSet) => {
    _saveConfiguration(history, newIndexSet);

    sendTelemetry('form_submit', {
      app_pathname: 'indexsets',
      app_section: 'indexset',
      app_action_value: 'configuration-form',
    });
  };

  return (
    <DocumentTitle title="Configure Index Set">
      <div>
        <PageHeader title="Configure Index Set"
                    documentationLink={{
                      title: 'Index model documentation',
                      path: DocsHelper.PAGES.INDEX_MODEL,
                    }}
                    topActions={(
                      <LinkContainer to={Routes.SYSTEM.INDICES.LIST}>
                        <Button bsStyle="info">Index sets overview</Button>
                      </LinkContainer>
                    )}>
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
