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

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import ConfigurationHelper from 'components/sidecars/configuration-forms/ConfigurationHelper';
import history from 'util/History';
import withParams from 'routing/withParams';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

const SidecarEditConfigurationPage = createReactClass({
  displayName: 'SidecarEditConfigurationPage',

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
        <span>
          <PageHeader title="Collector Configuration">
            <span>
              Some words about collector configurations.
            </span>

            <span>
              Read more about the Graylog Sidecar in the documentation.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
                <Button bsStyle="info">Overview</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
                <Button bsStyle="info">Administration</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
                <Button bsStyle="info" className="active">Configuration</Button>
              </LinkContainer>
            </ButtonToolbar>
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
        </span>
      </DocumentTitle>
    );
  },
});

export default withParams(SidecarEditConfigurationPage);
