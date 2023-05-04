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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import connect from 'stores/connect';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import { IndexSetsActions, IndexSetsStore, IndexSetPropType } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';
import { RetentionStrategyPropType, RotationStrategyPropType } from 'components/indices/Types';
import withHistory from 'routing/withHistory';
import withTelemetry from 'logic/telemetry/withTelemetry';

const _saveConfiguration = (history, indexSet) => IndexSetsActions.update(indexSet).then(() => {
  history.push(Routes.SYSTEM.INDICES.LIST);
});

class IndexSetConfigurationPage extends React.Component {
  componentDidMount() {
    IndexSetsActions.get(this.props.params.indexSetId);
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  }

  _formCancelLink = () => {
    const { location: { query: { from } }, indexSet } = this.props;

    if (from === 'details') {
      return Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id);
    }

    return Routes.SYSTEM.INDICES.LIST;
  };

  _isLoading = () => {
    const { indexSet, rotationStrategies, retentionStrategies } = this.props;

    return !indexSet || !rotationStrategies || !retentionStrategies;
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const {
      indexSet,
      retentionStrategiesContext,
      rotationStrategies,
      retentionStrategies,
      history,
      sendTelemetry,
    } = this.props;

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
                                         cancelLink={this._formCancelLink()}
                                         onUpdate={saveConfiguration} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  }
}

IndexSetConfigurationPage.propTypes = {
  params: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  retentionStrategies: PropTypes.arrayOf(RetentionStrategyPropType),
  rotationStrategies: PropTypes.arrayOf(RotationStrategyPropType),
  indexSet: IndexSetPropType,
  retentionStrategiesContext: PropTypes.shape({
    max_index_retention_period: PropTypes.string,
  }),
  history: PropTypes.object.isRequired,
  sendTelemetry: PropTypes.func.isRequired,
};

IndexSetConfigurationPage.defaultProps = {
  retentionStrategies: undefined,
  rotationStrategies: undefined,
  indexSet: undefined,
  retentionStrategiesContext: {
    max_index_retention_period: undefined,
  },
};

export default connect(
  withHistory(withParams(withLocation(withTelemetry(IndexSetConfigurationPage)))),
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
