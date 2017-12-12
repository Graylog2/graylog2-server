import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import _ from 'lodash';

const FieldList = createReactClass({
  propTypes: {
    job: PropTypes.object.isRequired,
    selectedQuery: PropTypes.object.isRequired,
  },

  render() {
    const job = this.props.job;
    if (!job) {
      return null;
    }
    const queryResult = job.results[this.props.selectedQuery];
    if (!queryResult) {
      return null;
    }
    // TODO this requires that each query actually has a message list available.
    let content = null;
    const searchTypes = queryResult.searchTypes;
    // TODO how do we deal with multiple lists? is that even useful?
    const messagesSearchType = _.find(searchTypes, t => t.type === 'messages');
    if (!messagesSearchType) {
      content = <span>No field information available.</span>;
    } else {
      const fields = [];
      messagesSearchType.fields.forEach((count, name) => {
        // eslint-disable-next-line react/no-array-index-key
        fields.push(<dt key={`t-${name}`}>{name}</dt>);
        // eslint-disable-next-line react/no-array-index-key
        fields.push(<dd key={`d-${name}`}>{count}</dd>);
      });
      content = (<dl>
        {fields}
      </dl>);
    }
    return (
      <div>
        <h3>Fields</h3>
        {content}
      </div>
    );
  },
});

export default FieldList;
