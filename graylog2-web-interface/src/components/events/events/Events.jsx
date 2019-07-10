import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';

import { DataTable, PaginatedList, SearchForm, Timestamp } from 'components/common';

import styles from './Events.css';

class Events extends React.Component {
  static propTypes = {
    events: PropTypes.array.isRequired,
    parameters: PropTypes.object.isRequired,
    totalEvents: PropTypes.number.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
  };

  eventsFormatter = (event) => {
    return (
      <tr key={event.id}>
        <td><Timestamp dateTime={event.timestamp} /></td>
        <td>{event.message}</td>
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
                       headers={['Timestamp', 'Message']}
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
