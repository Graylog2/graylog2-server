import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import history from 'util/History';
import CombinedProvider from 'injection/CombinedProvider';

import CollectorForm from 'components/sidecars/configuration-forms/CollectorForm';

const { CollectorsActions } = CombinedProvider.get('Collectors');

const SidecarEditCollectorPage = createReactClass({
  displayName: 'SidecarEditCollectorPage',

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
        <span>
          <PageHeader title="Log Collector">
            <span>
              Some words about log collectors.
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
              <CollectorForm action="edit" collector={this.state.collector} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default SidecarEditCollectorPage;
