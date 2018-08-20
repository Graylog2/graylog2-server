import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import ConfigurationForm from 'components/sidecars/configuration-forms/ConfigurationForm';
import ConfigurationHelper from 'components/sidecars/configuration-forms/ConfigurationHelper';

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
    this.style.use();
    this._reloadConfiguration();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!components/sidecars/styles/SidecarStyles.css'),

  _reloadConfiguration() {
    const configurationId = this.props.params.configurationId;
    CollectorConfigurationsActions.getConfiguration(configurationId).then(this._setConfiguration);
    CollectorConfigurationsActions.getConfigurationSidecars(configurationId).then(this._setConfigurationSidecars);
  },

  _setConfiguration(configuration) {
    this.setState({ configuration });
  },

  _setConfigurationSidecars(configurationSidecars) {
    this.setState({ configurationSidecars });
  },

  _isLoading() {
    return !(this.state.configuration);
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
              <ConfigurationForm configuration={this.state.configuration}
                                 configurationSidecars={this.state.configurationSidecars} />
            </Col>
            <Col md={6}>
              <ConfigurationHelper type="filebeat" />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default SidecarEditConfigurationPage;
