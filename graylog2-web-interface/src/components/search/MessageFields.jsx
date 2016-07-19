import React from 'react';
import Immutable from 'immutable';

import { MessageField } from 'components/search';

const MessageFields = React.createClass({
  propTypes: {
    customFieldActions: React.PropTypes.node,
    disableFieldActions: React.PropTypes.bool,
    message: React.PropTypes.object.isRequired,
    possiblyHighlight: React.PropTypes.func.isRequired,
  },
  render() {
    const formattedFields = Immutable.Map(this.props.message.formatted_fields).sortBy((value, key) => key, (fieldA, fieldB) => fieldA.localeCompare(fieldB));
    const fields = formattedFields.map((value, key) => <MessageField fieldName={key} value={value} {...this.props} />);

    return (
      <dl className="message-details message-details-fields">
        {fields}
      </dl>
    );
  },
});

export default MessageFields;
