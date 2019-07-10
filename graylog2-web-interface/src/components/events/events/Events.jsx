import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import { Link } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DataTable, PaginatedList, SearchForm, Timestamp } from 'components/common';
import Routes from 'routing/Routes';

import styles from './Events.css';

class Events extends React.Component {
  static propTypes = {
    events: PropTypes.array.isRequired,
    parameters: PropTypes.object.isRequired,
    totalEvents: PropTypes.number.isRequired,
    context: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find(edt => edt.type === type);
  };

  eventDefinitionFormatter = (event, eventDefinitionContext) => {
    const plugin = this.getConditionPlugin(event.event_definition_type);
    return (
      <React.Fragment>
        <Link to={Routes.NEXT_ALERTS.DEFINITIONS.show(eventDefinitionContext.id)}>
          {eventDefinitionContext.title}
        </Link>
        &emsp;
        ({plugin.displayName || event.event_definition_type})
      </React.Fragment>
    );
  };

  eventsFormatter = (event) => {
    const { context } = this.props;

    return (
      <tr key={event.id}>
        <td><Timestamp dateTime={event.timestamp} /></td>
        <td>{event.message}</td>
        <td>{this.eventDefinitionFormatter(event, context.event_definitions[event.event_definition_id])}</td>
      </tr>
    );
  };

  render() {
    const { events, parameters, totalEvents, onPageChange, onQueryChange } = this.props;

    const eventList = events.map(e => e.event);
    return (
      <Row>
        <Col md={12}>
          <SearchForm query={parameters.query}
                      onSearch={onQueryChange}
                      onReset={onQueryChange}
                      searchButtonLabel="Find"
                      placeholder="Find Events"
                      wrapperClass={styles.inline}
                      queryWidth={200}
                      topMargin={0}
                      useLoadingState />

          <PaginatedList activePage={parameters.page}
                         pageSize={parameters.pageSize}
                         pageSizes={[10, 25, 50]}
                         totalItems={totalEvents}
                         onChange={onPageChange}>
            <DataTable id="events-table"
                       className="table-striped table-hover"
                       headers={['Timestamp', 'Message', 'Event Definition']}
                       rows={eventList}
                       dataRowFormatter={this.eventsFormatter}
                       filterKeys={[]} />
          </PaginatedList>
        </Col>
      </Row>
    );
  }
}

export default Events;
