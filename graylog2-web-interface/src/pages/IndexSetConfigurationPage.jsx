import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, Row, Col } from 'react-bootstrap';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { DocumentationLink } from 'components/support';

import CombinedProvider from 'injection/CombinedProvider';

const { IndexSetsStore, IndexSetsActions } = CombinedProvider.get('IndexSets');
const { IndicesConfigurationStore, IndicesConfigurationActions } = CombinedProvider.get('IndicesConfiguration');

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

const IndexSetConfigurationPage = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired,
    location: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(IndexSetsStore), Reflux.connect(IndicesConfigurationStore)],

  getInitialState() {
    return {
      indexSet: undefined,
    };
  },

  componentDidMount() {
    IndexSetsActions.get(this.props.params.indexSetId);
    IndicesConfigurationActions.loadRotationStrategies();
    IndicesConfigurationActions.loadRetentionStrategies();
  },

  _formCancelLink() {
    if (this.props.location.query.from === 'details') {
      return Routes.SYSTEM.INDEX_SETS.SHOW(this.state.indexSet.id);
    }

    return Routes.SYSTEM.INDICES.LIST;
  },

  _saveConfiguration(indexSet) {
    IndexSetsActions.update(indexSet).then(() => {
      this.props.history.pushState(null, Routes.SYSTEM.INDICES.LIST);
    });
  },

  _isLoading() {
    return !this.state.indexSet || !this.state.rotationStrategies || !this.state.retentionStrategies;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const indexSet = this.state.indexSet;

    return (
      <DocumentTitle title="Configure Index Set">
        <div>
          <PageHeader title="Configure Index Set">
            <span>
              Modify the current configuration for this index set, allowing you to customize the retention, sharding,
              and replication of messages coming from one or more streams.
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
                                         cancelLink={this._formCancelLink()}
                                         onUpdate={this._saveConfiguration} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default IndexSetConfigurationPage;
