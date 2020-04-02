import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import ConfigurationHelper from 'components/sidecars/configuration-forms/ConfigurationHelper';
import history from 'util/History';

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

export default SidecarEditConfigurationPage;
