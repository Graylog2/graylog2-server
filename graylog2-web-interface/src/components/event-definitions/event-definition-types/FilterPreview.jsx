import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'react-bootstrap';

import { Spinner } from 'components/common';

import styles from './FilterPreview.css';

class FilterPreview extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    searchResult: PropTypes.object,
    isFetchingData: PropTypes.bool,
  };

  static defaultProps = {
    searchResult: {},
    isFetchingData: false,
  };

  renderMessages = (messages) => {
    return messages.map(({ message }) => {
      return (
        <tr key={message._id}>
          <td>{message.timestamp}</td>
          <td>{message.message}</td>
        </tr>
      );
    });
  };

  renderSearchResult = (searchResult) => {
    if (!searchResult) {
      return <p>Start configuring a Condition and a preview of the results will appear here.</p>;
    }

    return (
      <Table striped hover condensed>
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody>
          {this.renderMessages(searchResult.messages)}
        </tbody>
      </Table>
    );
  };

  render() {
    const { isFetchingData, searchResult } = this.props;

    return (
      <div className={styles.filterPreview}>
        <h3>Filter preview</h3>
        { isFetchingData ? <Spinner text="Loading filter preview..." /> : this.renderSearchResult(searchResult) }
      </div>
    );
  }
}

export default FilterPreview;
