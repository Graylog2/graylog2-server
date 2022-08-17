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

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { DocumentationLink } from 'components/support';
import DateTime from 'logic/datetimes/DateTime';
import history from 'util/History';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import connect from 'stores/connect';
import { IndexSetPropType, IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import { RetentionStrategyPropType, RotationStrategyPropType } from 'components/indices/Types';
import type { RetentionStrategy, RotationStrategy, RetentionStrategyContext } from 'components/indices/Types';
import useIndexDefaults from 'pages/useIndexDefaults';
import {IndexConfig} from 'components/configurations/IndexSetsDefaultsConfig';

type Props = {
  indexSet: Partial<IndexSet> | null | undefined,
  retentionStrategies?: Array<RetentionStrategy> | null | undefined,
  rotationStrategies?: Array<RotationStrategy> | null | undefined,
  retentionStrategiesContext?: RetentionStrategyContext | null | undefined,
}

const fallbackDefaults : IndexConfig =  {
  index_prefix: '',
  index_analyzer: 'standard',
  shards: 4,
  replicas: 0,
  index_optimization_max_num_segments: 1,
  index_optimization_disabled: false,
  field_type_refresh_interval: 5,
  field_type_refresh_interval_unit: "SECONDS" }

const IndexSetCreationPage = ({ retentionStrategies, rotationStrategies, retentionStrategiesContext, indexSet }: Props) => {

  const { isLoading, config } = useIndexDefaults(fallbackDefaults);

  useEffect(() => {
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  }, []);

  const _saveConfiguration = (indexSetItem: IndexSet) => {
    const copy = indexSetItem;

    copy.creation_date = DateTime.now().toISOString();

    IndexSetsActions.create(copy).then(() => {
      history.push(Routes.SYSTEM.INDICES.LIST);
    });
  };

  const _isLoading = () => {
    return !rotationStrategies || !retentionStrategies;
  };

  if (_isLoading()) {
    return <Spinner />;
  }

  const defaultIndexSet = {
    ...indexSet,
    rotation_strategy_class: rotationStrategies[0].type,
    rotation_strategy: rotationStrategies[0].default_config,
  };

  return (
    <DocumentTitle title="Create Index Set">
      <div>
        <PageHeader title="Create Index Set">
          <span>
            Create a new index set that will let you configure the retention, sharding, and replication of messages
            coming from one or more streams.
          </span>
          <span>
            You can learn more about the index model in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />
          </span>
          <span>
            <LinkContainer to={Routes.SYSTEM.INDICES.LIST}>
              <Button bsStyle="info">Index sets overview</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IndexSetConfigurationForm indexSet={defaultIndexSet}
                                       retentionStrategiesContext={retentionStrategiesContext}
                                       rotationStrategies={rotationStrategies}
                                       retentionStrategies={retentionStrategies}
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
  indexSet: IndexSetPropType,
  retentionStrategiesContext: PropTypes.shape({
    max_index_retention_period: PropTypes.string,
  }),
};

IndexSetCreationPage.defaultProps = {
  retentionStrategies: undefined,
  rotationStrategies: undefined,
  indexSet: {
    title: '',
    description: '',
    index_prefix: '',
    writable: true,
    can_be_default: true,
    shards: 4,
    replicas: 0,
    retention_strategy_class: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
    retention_strategy: {
      max_number_of_indices: 20,
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
    },
    index_analyzer: 'standard',
    index_optimization_max_num_segments: 1,
    index_optimization_disabled: false,
    field_type_refresh_interval: 5 * 1000, // 5 seconds
  },
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
