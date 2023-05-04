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
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import connect from 'stores/connect';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import {
  RetentionStrategyPropType,
  RotationStrategyPropType,
} from 'components/indices/Types';
import type {
  RetentionStrategy, RotationStrategy, RetentionStrategyContext,
  RetentionStrategyConfig,
  RotationStrategyConfig,
} from 'components/indices/Types';
import { adjustFormat } from 'util/DateTime';
import useIndexDefaults from 'pages/useIndexDefaults';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

type Props = {
  retentionStrategies?: Array<RetentionStrategy> | null | undefined,
  rotationStrategies?: Array<RotationStrategy> | null | undefined,
  retentionStrategiesContext?: RetentionStrategyContext | null | undefined,
}

const IndexSetCreationPage = ({ retentionStrategies, rotationStrategies, retentionStrategiesContext }: Props) => {
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  }, []);

  const _saveConfiguration = (indexSetItem: IndexSet) => {
    sendTelemetry('form_submit', {
      app_pathname: 'indexsets',
      app_action_value: 'createindexset',
    });

    const copy = indexSetItem;

    copy.creation_date = adjustFormat(new Date(), 'internal');

    return IndexSetsActions.create(copy).then(() => {
      history.push(Routes.SYSTEM.INDICES.LIST);
    });
  };

  const { loadingIndexDefaultsConfig, indexDefaultsConfig: config } = useIndexDefaults();

  const _isLoading = () => {
    return !rotationStrategies || !retentionStrategies || loadingIndexDefaultsConfig;
  };

  if (_isLoading()) {
    return <Spinner />;
  }

  const indexSet: IndexSet = {
    title: '',
    description: '',
    index_prefix: config.index_prefix,
    writable: true,
    can_be_default: true,
    shards: config.shards,
    replicas: config.replicas,
    rotation_strategy_class: config.rotation_strategy_class,
    rotation_strategy: config.rotation_strategy_config as RotationStrategyConfig,
    retention_strategy_class: config.retention_strategy_class,
    retention_strategy: config.retention_strategy_config as RetentionStrategyConfig,
    index_analyzer: config.index_analyzer,
    index_optimization_max_num_segments: config.index_optimization_max_num_segments,
    index_optimization_disabled: config.index_optimization_disabled,
    field_type_refresh_interval: moment.duration(config.field_type_refresh_interval, config.field_type_refresh_interval_unit).asMilliseconds(),
  };

  return (
    <DocumentTitle title="Create Index Set">
      <div>
        <PageHeader title="Create Index Set"
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
            Create a new index set that will let you configure the retention, sharding, and replication of messages
            coming from one or more streams.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IndexSetConfigurationForm indexSet={indexSet}
                                       retentionStrategiesContext={retentionStrategiesContext}
                                       rotationStrategies={rotationStrategies}
                                       retentionStrategies={retentionStrategies}
                                       submitButtonText="Create index set"
                                       submitLoadingText="Creating index set..."
                                       create
                                       cancelLink={Routes.SYSTEM.INDICES.LIST}
                                       onUpdate={_saveConfiguration} />
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

IndexSetCreationPage.propTypes = {
  retentionStrategies: PropTypes.arrayOf(RetentionStrategyPropType),
  rotationStrategies: PropTypes.arrayOf(RotationStrategyPropType),
  retentionStrategiesContext: PropTypes.shape({
    max_index_retention_period: PropTypes.string,
  }),
};

IndexSetCreationPage.defaultProps = {
  retentionStrategies: undefined,
  rotationStrategies: undefined,
  retentionStrategiesContext: {
    max_index_retention_period: undefined,
  },
};

export default connect(
  IndexSetCreationPage,
  {
    indexSets: IndexSetsStore,
    indicesConfigurations: IndicesConfigurationStore,
  },
  ({ indexSets, indicesConfigurations }) => ({
    indexSet: indexSets.indexSet,
    rotationStrategies: indicesConfigurations.rotationStrategies,
    retentionStrategies: indicesConfigurations.retentionStrategies,
    retentionStrategiesContext: indicesConfigurations.retentionStrategiesContext,
  }),
);
