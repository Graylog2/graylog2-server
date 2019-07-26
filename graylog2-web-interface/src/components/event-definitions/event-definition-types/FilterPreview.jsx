import React from 'react';
import PropTypes from 'prop-types';
import { Panel, Table } from 'react-bootstrap';

import { Spinner } from 'components/common';
import HelpPanel from 'components/event-definitions/common/HelpPanel';

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
      <React.Fragment>
        <HelpPanel collapsible
                   title="How many Events will Filter & Aggregation create?">
          <p>
            The Filter & Aggregation Condition will generate different number of Events, depending on how it is
            configured:
          </p>
          <ul>
            <li><b>Filter:</b>&emsp;One Event per message matching the filter</li>
            <li><b>Aggregation without groups:</b>&emsp;One Event every time the Event Definition is executed</li>
            <li><b>Aggregation with groups:</b>&emsp;One Event per group every time the Event Definition is executed
            </li>
          </ul>
        </HelpPanel>

        <Panel className={styles.filterPreview} header={<h3>Filter Preview</h3>}>
          {errors.length > 0 ? (
            <p className="text-danger">{errors[0].description}</p>
          ) : (
            isFetchingData ? <Spinner text="Loading filter preview..." /> : this.renderSearchResult(searchResult)
          )
          }
        </Panel>
      </React.Fragment>
    );
  }
}

export default FilterPreview;
