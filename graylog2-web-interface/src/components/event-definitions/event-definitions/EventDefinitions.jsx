import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';
import moment from 'moment';
import {} from 'moment-duration-format';

import { Button, Col, DropdownButton, MenuItem, Row } from 'components/graylog';
import Routes from 'routing/Routes';

import {
  EmptyEntity,
  EntityList,
  EntityListItem,
  IfPermitted,
  PaginatedList,
  Pluralize,
  SearchForm,
} from 'components/common';

import styles from './EventDefinitions.css';

class EventDefinitions extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
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

  renderDescription = (definition) => {
    let schedulingInformation = 'Not scheduled.';
    if (definition.config.search_within_ms && definition.config.execute_every_ms) {
      const executeEveryFormatted = moment.duration(definition.config.execute_every_ms)
        .format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all', usePlural: false });
      const searchWithinFormatted = moment.duration(definition.config.search_within_ms)
        .format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all' });
      schedulingInformation = `Runs every ${executeEveryFormatted}, searching within the last ${searchWithinFormatted}.`;
    }

    let notificationsInformation = <span>Does <b>not</b> trigger any Notifications.</span>;
    if (definition.notifications.length > 0) {
      notificationsInformation = (
        <span>
          Triggers {definition.notifications.length}{' '}
          <Pluralize singular="Notification" plural="Notifications" value={definition.notifications.length} />.
        </span>
      );
    }

    return (
      <>
        <p>{definition.description}</p>
        <p>{schedulingInformation} {notificationsInformation}</p>
      </>
    );
  };

  render() {
    const { eventDefinitions, pagination, query, onPageChange, onQueryChange, onDelete } = this.props;

    if (pagination.grandTotal === 0) {
      return this.renderEmptyContent();
    }

    const items = eventDefinitions.map((definition) => {
      const actions = (
        <React.Fragment key={`actions-${definition.id}`}>
          <IfPermitted permissions={`eventdefinitions:edit:${definition.id}`}>
            <LinkContainer to={Routes.ALERTS.DEFINITIONS.edit(definition.id)}>
              <Button bsStyle="info">Edit</Button>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions={`eventdefinitions:delete:${definition.id}`}>
            <DropdownButton id="more-dropdown" title="More" pullRight>
              <MenuItem onClick={onDelete(definition)}>Delete</MenuItem>
            </DropdownButton>
          </IfPermitted>
        </React.Fragment>
      );

      const plugin = this.getConditionPlugin(definition.config.type);
      const titleSuffix = plugin.displayName || definition.config.type;
      return (
        <EntityListItem key={`event-definition-${definition.id}`}
                        title={definition.title}
                        titleSuffix={titleSuffix}
                        description={this.renderDescription(definition)}
                        noItemsText="Could not find any items with the given filter."
                        actions={actions} />
      );
    });

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
