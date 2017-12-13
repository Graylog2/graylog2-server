import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import _ from 'lodash';

import Field from 'enterprise/components/Field';

const FieldList = createReactClass({
  propTypes: {
    job: PropTypes.object.isRequired,
    selectedQuery: PropTypes.string.isRequired,
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
      content = (<ul style={{ padding: 0 }}>
        {
          messagesSearchType.fields.entrySeq().map(([name]) => {
            return <li key={`field-${name}`}><Field name={name} interactive /></li>;
          })
        }
      </ul>);
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
