import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, DropdownButton, MenuItem, Row } from 'react-bootstrap';
import lodash from 'lodash';

import { DocumentTitle, EmptyEntity, EntityList, EntityListItem, PageHeader } from 'components/common';
import EventExecutionContainer from 'components/event-definitions/event-execution-forms/EventExecutionContainer';
import DocumentationLink from 'components/support/DocumentationLink';

import connect from 'stores/connect';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';

const { EventDefinitionsStore, EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

class EventDefinitionsPage extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
  };

  state = {
    executeDefinition: undefined,
  };

  componentDidMount() {
    EventDefinitionsActions.list();
  }

  handleDelete = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete "${definition.title}"?`)) {
        EventDefinitionsActions.delete(definition);
      }
    };
  };

  handleExecute = (definition) => {
    return () => this.setState({ executeDefinition: definition });
  };

  handleExecutionSubmit = (eventDefinition, payload) => {
    EventDefinitionsActions.execute(eventDefinition, payload)
      .then(() => {
        this.setState({ executeDefinition: null });
      });
  };

  handleExecutionCancellation = () => {
    this.setState({ executeDefinition: null });
  };

  renderEmptyContent = () => {
    return (
      <Row>
        <Col md={4} mdOffset={4}>
          <EmptyEntity>
            <p>
              Create Event Definitions that are able to search, aggregate or correlate Messages and other
              Events, allowing you to record significant Events in Graylog and alert on them.
            </p>
            <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.CREATE}>
              <Button bsStyle="success">Get Started!</Button>
            </LinkContainer>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };

  renderContent = () => {
    const { eventDefinitions } = this.props;
    const definitions = eventDefinitions.list;

    if (eventDefinitions.list.length < 1) {
      return this.renderEmptyContent();
    }

    const items = definitions.map((definition) => {
      const actions = [
        <LinkContainer key={`edit-button-${definition.id}`}
                       to={Routes.NEXT_ALERTS.DEFINITIONS.edit(definition.id)}>
          <Button bsStyle="info">Edit</Button>
        </LinkContainer>,
        <DropdownButton key={`actions-${definition.id}`} id="more-dropdown" title="More" pullRight>
          <MenuItem onClick={this.handleExecute(definition)}>Execute</MenuItem>
          <MenuItem divider />
          <MenuItem onClick={this.handleDelete(definition)}>Delete</MenuItem>
        </DropdownButton>,
      ];

      // TODO: Show something more useful ;)
      const titleSuffix = lodash.get(definition, 'config.type', '')
        .replace(/-v\d+/, '');
      return (
        <EntityListItem key={`event-definition-${definition.id}`}
                        title={definition.title}
                        titleSuffix={titleSuffix}
                        description={definition.description}
                        actions={actions} />
      );
    });

    return (<EntityList items={items} />);
  };

  render() {
    const { executeDefinition } = this.state;

    return (
      <DocumentTitle title="Event Definitions">
        <span>
          <PageHeader title="Event Definitions">
            <span>
              Create new Event Definitions that will allow you to search for different Conditions and alert on them.
            </span>

            <span>
              Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                                 text="documentation" />
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success">Create Event Definition</Button>
              </LinkContainer>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info" className="active">Event Definitions</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <Row>
                <Col md={12}>
                  {this.renderContent()}
                  <EventExecutionContainer eventDefinition={executeDefinition}
                                           onSubmit={this.handleExecutionSubmit}
                                           onCancel={this.handleExecutionCancellation} />
                </Col>
              </Row>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default connect(EventDefinitionsPage, { eventDefinitions: EventDefinitionsStore });
