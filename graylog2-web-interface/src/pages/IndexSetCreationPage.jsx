import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, Row, Col } from 'react-bootstrap';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { DocumentationLink } from 'components/support';
import DateTime from 'logic/datetimes/DateTime';

import CombinedProvider from 'injection/CombinedProvider';

const { IndexSetsStore, IndexSetsActions } = CombinedProvider.get('IndexSets');
const { IndicesConfigurationStore, IndicesConfigurationActions } = CombinedProvider.get('IndicesConfiguration');

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

const IndexSetCreationPage = React.createClass({
  propTypes: {
    history: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(IndexSetsStore), Reflux.connect(IndicesConfigurationStore)],

  getInitialState() {
    return {
      indexSet: {
        title: '',
        description: '',
        index_prefix: '',
        writable: true,
        shards: 4,
        replicas: 0,
        retention_strategy_class: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
        retention_strategy: {
          max_number_of_indices: 20,
          type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
        },
        rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
        rotation_strategy: {
          max_docs_per_index: 20000000,
          type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
        },
        index_analyzer: 'standard',
        index_optimization_max_num_segments: 1,
        index_optimization_disabled: false,
      },
    };
  },

  componentDidMount() {
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  },

  _saveConfiguration(indexSet) {
    const copy = indexSet;
    copy.creation_date = DateTime.now().toISOString();
    IndexSetsActions.create(copy).then(() => {
      this.props.history.pushState(null, Routes.SYSTEM.INDICES.LIST);
    });
  },

  _isLoading() {
    return !this.state.rotationStrategies || !this.state.retentionStrategies;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const indexSet = this.state.indexSet;

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
              <IndexSetConfigurationForm indexSet={indexSet}
                                         rotationStrategies={this.state.rotationStrategies}
                                         retentionStrategies={this.state.retentionStrategies}
                                         create
                                         cancelLink={Routes.SYSTEM.INDICES.LIST}
                                         onUpdate={this._saveConfiguration} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default IndexSetCreationPage;
