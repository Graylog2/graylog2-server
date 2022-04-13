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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { IndexSetConfigurationForm } from 'components/indices';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import history from 'util/History';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import { IndicesConfigurationActions, IndicesConfigurationStore } from 'stores/indices/IndicesConfigurationStore';

const IndexSetConfigurationPage = createReactClass({
  displayName: 'IndexSetConfigurationPage',

  propTypes: {
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
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
      history.push(Routes.SYSTEM.INDICES.LIST);
    });
  },

  _isLoading() {
    return !this.state.indexSet || !this.state.rotationStrategies || !this.state.retentionStrategies;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { indexSet } = this.state;

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
                                         retentionStrategiesContext={this.state.retentionStrategiesContext}
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

export default withParams(withLocation(IndexSetConfigurationPage));
