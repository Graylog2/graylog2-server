import PropTypes from 'prop-types';
import React from 'react';

import { MessageField } from 'components/search';

class MessageFields extends React.Component {
  static propTypes = {
    customFieldActions: PropTypes.node,
    message: PropTypes.object.isRequired,
    renderForDisplay: PropTypes.func.isRequired,
  };

  static defaultProps = {
    customFieldActions: undefined,
  };

  _formatFields = (fields) => {
    return Object.keys(fields)
      .sort()
      .map((key) => {
        return (
          <MessageField key={key}
                        {...this.props}
                        fieldName={key}
                        value={fields[key]} />
        );
      });
  };

  render() {
    const { message } = this.props;
    const formattedFields = message.formatted_fields;
    const fields = this._formatFields(formattedFields);

    return (
      <dl className="message-details message-details-fields">
        {fields}
      </dl>
    );
  }
}

export default MessageFields;
