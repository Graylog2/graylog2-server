import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, DropdownButton, MenuItem, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import lodash from 'lodash';

import Routes from 'routing/Routes';

import { EmptyEntity, EntityList, EntityListItem, } from 'components/common';

class EventDefinitions extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
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

  render() {
    const { eventDefinitions, onDelete } = this.props;
    const definitions = eventDefinitions.list;

    if (eventDefinitions.list.length === 0) {
      return this.renderEmptyContent();
    }

    const items = definitions.map((definition) => {
      const actions = [
        <LinkContainer key={`edit-button-${definition.id}`}
                       to={Routes.NEXT_ALERTS.DEFINITIONS.edit(definition.id)}>
          <Button bsStyle="info">Edit</Button>
        </LinkContainer>,
        <DropdownButton key={`actions-${definition.id}`} id="more-dropdown" title="More" pullRight>
          <MenuItem onClick={onDelete(definition)}>Delete</MenuItem>
        </DropdownButton>,
      ];

      // TODO: Show something more useful ;)
      const titleSuffix = lodash.get(definition, 'config.type', 'Not available')
        .replace(/-v\d+/, '');
      return (
        <EntityListItem key={`event-definition-${definition.id}`}
                        title={definition.title}
                        titleSuffix={`${titleSuffix}`}
                        description={definition.description}
                        actions={actions} />
      );
    });

    return (<EntityList items={items} />);
  }
}

export default EventDefinitions;
