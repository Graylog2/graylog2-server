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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import history from 'util/History';
import { CollectorsActions } from 'stores/sidecars/CollectorsStore';
import CollectorForm from 'components/sidecars/configuration-forms/CollectorForm';
import withParams from 'routing/withParams';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';
import DocsHelper from 'util/DocsHelper';

const SidecarEditCollectorPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'SidecarEditCollectorPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      collector: undefined,
    };
  },

  componentDidMount() {
    this._reloadCollector();
  },

  _reloadCollector() {
    CollectorsActions.getCollector(this.props.params.collectorId).then(
      (collector) => this.setState({ collector }),
      (error) => {
        if (error.status === 404) {
          history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION);
        }
      },
    );
  },

  _isLoading() {
    return !(this.state.collector);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Log Collector">
        <SidecarsPageNavigation />
        <PageHeader title="Log Collector"
                    documentationLink={{
                      title: 'Sidecar documentation',
                      path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                    }}>
          <span>
            Some words about log collectors.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={6}>
            <CollectorForm action="edit" collector={this.state.collector} />
          </Col>
        </Row>
      </DocumentTitle>
    );
  },
});

export default withParams(SidecarEditCollectorPage);
