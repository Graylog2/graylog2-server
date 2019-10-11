import PropTypes from 'prop-types';
import React from 'react';

import createReactClass from 'create-react-class';

import { MessageFieldDescription } from 'components/search';
import DecorationStats from 'logic/message/DecorationStats';

const MessageField = createReactClass({
  displayName: 'MessageField',

  propTypes: {
    customFieldActions: PropTypes.node,
    disableFieldActions: PropTypes.bool,
    fieldName: PropTypes.string.isRequired,
    message: PropTypes.object.isRequired,
    renderForDisplay: PropTypes.func.isRequired,
    value: PropTypes.any.isRequired,
  },

  SPECIAL_FIELDS: ['full_message', 'level'],

  render() {
    const { message } = this.props;
    let innerValue = this.props.value;
    const key = this.props.fieldName;
    if (this.SPECIAL_FIELDS.indexOf(key) !== -1) {
      innerValue = message.fields[key];
    }

    return (
      <span>
        <dt key={`${key}Title`}>{key}</dt>
        <MessageFieldDescription key={`${key}Description`}
                                 message={message}
                                 fieldName={key}
                                 fieldValue={innerValue}
                                 renderForDisplay={this.props.renderForDisplay}
                                 disableFieldActions={DecorationStats.isFieldAddedByDecorator(message, key) || this.props.disableFieldActions}
                                 customFieldActions={this.props.customFieldActions} />
      </span>
    );
  },
});

export default MessageField;
