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
import naturalSort from 'javascript-natural-sort';

import { Row, Col, Button } from 'components/graylog';
import Spinner from 'components/common/Spinner';
import AddExtractorWizard from 'components/extractors/AddExtractorWizard';
import EntityList from 'components/common/EntityList';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';

import ExtractorsListItem from './ExtractorsListItem';
import ExtractorsSortModal from './ExtractorSortModal';

const ExtractorsActions = ActionsProvider.getActions('Extractors');
const ExtractorsStore = StoreProvider.getStore('Extractors');

const ExtractorsList = createReactClass({
  displayName: 'ExtractorsList',

  propTypes: {
    input: PropTypes.object.isRequired,
    node: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(ExtractorsStore), Reflux.ListenerMethods],

  componentDidMount() {
    ExtractorsActions.list.triggerPromise(this.props.input.id);
  },

  _formatExtractor(extractor) {
    return (
      <ExtractorsListItem key={extractor.id}
                          extractor={extractor}
                          inputId={this.props.input.id}
                          nodeId={this.props.node.node_id} />
    );
  },

  _isLoading() {
    return !this.state.extractors;
  },

  _openSortModal() {
    this.sortModal.open();
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    let sortExtractorsButton;

    if (this.state.extractors.length > 1) {
      sortExtractorsButton = (
        <Button bsSize="xsmall" bsStyle="primary" className="pull-right" onClick={this._openSortModal}>
          Sort extractors
        </Button>
      );
    }

    const formattedExtractors = this.state.extractors
      .sort((extractor1, extractor2) => naturalSort(extractor1.order, extractor2.order))
      .map(this._formatExtractor);

    return (
      <div>
        <AddExtractorWizard inputId={this.props.input.id} />
        <Row className="content extractor-list">
          <Col md={12}>
            <Row className="row-sm">
              <Col md={8}>
                <h2>Configured extractors</h2>
              </Col>
              <Col md={4}>
                {sortExtractorsButton}
              </Col>
            </Row>
            <EntityList bsNoItemsStyle="info"
                        noItemsText="This input has no configured extractors."
                        items={formattedExtractors} />
          </Col>
        </Row>
        <ExtractorsSortModal ref={(sortModal) => { this.sortModal = sortModal; }} input={this.props.input} extractors={this.state.extractors} />
      </div>
    );
  },
});

export default ExtractorsList;
