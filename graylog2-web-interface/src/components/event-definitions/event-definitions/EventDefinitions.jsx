import React from 'react';
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
import { Button, Col, Row } from 'components/graylog';
import Routes from 'routing/Routes';
import {
  EmptyEntity,
  EntityList,
  IfPermitted,
  PaginatedList,
  SearchForm,
} from 'components/common';

import styles from './EventDefinitions.css';
import EventDefinitionEntry from './EventDefinitionEntry';

class EventDefinitions extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.array.isRequired,
    context: PropTypes.object,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onEnable: PropTypes.func.isRequired,
    onDisable: PropTypes.func.isRequired,
  };

  static defaultProps = {
    context: {},
  };

  renderEmptyContent = () => {
    return (
      <Row>
        <Col md={6} mdOffset={3} lg={4} lgOffset={4}>
          <EmptyEntity>
            <p>
              Create Event Definitions that are able to search, aggregate or correlate Messages and other
              Events, allowing you to record significant Events in Graylog and alert on them.
            </p>
            <IfPermitted permissions="eventdefinitions:create">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success">Get Started!</Button>
              </LinkContainer>
            </IfPermitted>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };

  render() {
    const { eventDefinitions, context, pagination, query, onPageChange, onQueryChange, onDelete, onEnable, onDisable } = this.props;

    if (pagination.grandTotal === 0) {
      return this.renderEmptyContent();
    }

    const items = eventDefinitions.map((definition) => (
      <EventDefinitionEntry context={context}
                            eventDefinition={definition}
                            onDisable={onDisable}
                            onEnable={onEnable}
                            onDelete={onDelete} />
    ));

    return (
      <Row>
        <Col md={12}>
          <SearchForm query={query}
                      onSearch={onQueryChange}
                      onReset={onQueryChange}
                      searchButtonLabel="Find"
                      placeholder="Find Event Definitions"
                      wrapperClass={styles.inline}
                      queryWidth={200}
                      topMargin={0}
                      useLoadingState>
            <IfPermitted permissions="eventdefinitions:create">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success" className={styles.createButton}>Create Event Definition</Button>
              </LinkContainer>
            </IfPermitted>
          </SearchForm>

          <PaginatedList activePage={pagination.page}
                         pageSize={pagination.pageSize}
                         pageSizes={[10, 25, 50]}
                         totalItems={pagination.total}
                         onChange={onPageChange}>
            <div className={styles.definitionList}>
              <EntityList items={items} />
            </div>
          </PaginatedList>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitions;
