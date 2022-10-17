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
import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import ConfigurationHelper from 'components/sidecars/configuration-forms/ConfigurationHelper';
import history from 'util/History';
import withParams from 'routing/withParams';
import { CollectorConfigurationsActions } from 'stores/sidecars/CollectorConfigurationsStore';
import DocsHelper from 'util/DocsHelper';

import SidecarsSubareaNavigation from '../components/sidecars/common/SidecarsSubareaNavigation';

const SidecarEditConfigurationPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'SidecarEditConfigurationPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      configuration: undefined,
    };
  },

  componentDidMount() {
    this._reloadConfiguration();
  },

  _reloadConfiguration() {
    const { configurationId } = this.props.params;

    CollectorConfigurationsActions.getConfiguration(configurationId).then(
      (configuration) => {
        this.setState({ configuration: configuration });

        CollectorConfigurationsActions.getConfigurationSidecars(configurationId)
          .then((configurationSidecars) => this.setState({ configurationSidecars: configurationSidecars }));
      },
      (error) => {
        if (error.status === 404) {
          history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION);
        }
      },
    );
  },

  _isLoading() {
    return !this.state.configuration || !this.state.configurationSidecars;
  },

  _variableRenameHandler(oldname, newname) {
    this.configurationForm.replaceConfigurationVariableName(oldname, newname);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Collector Configuration">
        <SidecarsSubareaNavigation />
        <PageHeader title="Collector Configuration"
                    documentationLink={{
                      title: 'Sidecar documentation',
                      path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                    }}>
          <span>
            Some words about collector configurations.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={6}>
            <ConfigurationForm ref={(c) => { this.configurationForm = c; }}
                               configuration={this.state.configuration}
                               configurationSidecars={this.state.configurationSidecars} />
          </Col>
          <Col md={6}>
            <ConfigurationHelper onVariableRename={this._variableRenameHandler} />
          </Col>
        </Row>
      </DocumentTitle>
    );
  },
});

export default withParams(SidecarEditConfigurationPage);
