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

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { DocumentationLink } from 'components/support';
import DateTime from 'logic/datetimes/DateTime';
import history from 'util/History';
import CombinedProvider from 'injection/CombinedProvider';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import connect from 'stores/connect';
import {
  IndexSet,
  RetentionStrategy,
  RotationStrategy,
  IndicesConfigurationStoreState,
  RetentionStrategyPropType,
  RotationStrategyPropType,
  IndexSetsStoreState,
  IndexSetPropType,
} from 'components/indices/Types';
import { Store } from 'stores/StoreTypes';

const { IndexSetsActions, IndexSetsStore } = CombinedProvider.get('IndexSets');
const { IndicesConfigurationActions, IndicesConfigurationStore } = CombinedProvider.get('IndicesConfiguration');
type Props = {
  indexSet: Partial<IndexSet> | null | undefined,
  retentionStrategies?: Array<RetentionStrategy> | null | undefined,
  rotationStrategies?: Array<RotationStrategy> | null | undefined,
}

const IndexSetCreationPage = ({ retentionStrategies, rotationStrategies, indexSet }: Props) => {
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
    rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
    rotation_strategy: {
      max_docs_per_index: 20000000,
      type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
    },
    index_analyzer: 'standard',
    index_optimization_max_num_segments: 1,
    index_optimization_disabled: false,
    field_type_refresh_interval: 5 * 1000, // 5 seconds
  },
};

export default connect(
  IndexSetCreationPage,
  {
    indexSets: IndexSetsStore as Store<IndexSetsStoreState>,
    indicesConfigurations: IndicesConfigurationStore as Store<IndicesConfigurationStoreState>,
  },
  ({ indexSets, indicesConfigurations }) => ({
    indexSet: indexSets.indexSet,
    rotationStrategies: indicesConfigurations.rotationStrategies,
    retentionStrategies: indicesConfigurations.retentionStrategies,
  }),
);
