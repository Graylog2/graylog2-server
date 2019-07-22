import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'react-bootstrap';

import { Spinner } from 'components/common';

import styles from './FilterPreview.css';

class FilterPreview extends React.Component {
  static propTypes = {
    searchResult: PropTypes.object,
    errors: PropTypes.array,
    isFetchingData: PropTypes.bool,
  };

  static defaultProps = {
    searchResult: {},
    errors: [],
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
    if (searchResult.messages.length === 0) {
      return <p>Could not find any messages with the current search criteria.</p>;
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
    const { isFetchingData, searchResult, errors } = this.props;

    return (
      <div className={styles.filterPreview}>
        <h3>Filter preview</h3>
        {errors.length > 0 ? (
          <p className="text-danger">{errors[0].description}</p>
        ) : (
          isFetchingData ? <Spinner text="Loading filter preview..." /> : this.renderSearchResult(searchResult)
        )
        }
      </div>
    );
  }
}

export default FilterPreview;
