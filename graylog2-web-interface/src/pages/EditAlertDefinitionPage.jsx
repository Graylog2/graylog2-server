import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import AlertDefinitionFormContainer
  from 'components/alert-definitions/alert-definition-form/AlertDefinitionFormContainer';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertDefinitionsActions } = CombinedProvider.get('AlertDefinitions');

class EditAlertDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  state = {
    alertDefinition: undefined,
  };

  componentDidMount() {
    const { params } = this.props;
    AlertDefinitionsActions.get(params.definitionId)
      .then(alertDefinition => this.setState({ alertDefinition: alertDefinition }));
  }

  render() {
    const { alertDefinition } = this.state;

    if (!alertDefinition) {
      return (
        <DocumentTitle title="Edit Alert Definition">
          <span>
            <PageHeader title="Edit Alert Definition">
              <Spinner text="Loading Alert Definition..." />
            </PageHeader>
          </span>
        </DocumentTitle>
      );
    }

    return (
      <DocumentTitle title={`Edit "${alertDefinition.title}" Alert Definition`}>
        <span>
          <PageHeader title={`Edit "${alertDefinition.title}" Alert Definition`}>
            <span>
              Alert Definitions allow you to create Alerts from different Conditions and execute Actions on them.
            </span>

            <span>
              Alerts are our new alerting system that let you define more flexible rules and
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success">Create Alert Definition</Button>
              </LinkContainer>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info">Alert Definitions</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertDefinitionFormContainer action="edit" alertDefinition={alertDefinition} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EditAlertDefinitionPage;
