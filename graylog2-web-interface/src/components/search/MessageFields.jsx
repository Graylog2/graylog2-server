import PropTypes from 'prop-types';
import React from 'react';

import { MessageField } from 'components/search';

class MessageFields extends React.Component {
  static propTypes = {
    customFieldActions: PropTypes.node,
    disableFieldActions: PropTypes.bool,
    message: PropTypes.object.isRequired,
    renderForDisplay: PropTypes.func.isRequired,
  };

  static defaultProps = {
    showDecoration: false,
  };

  _formatFields = (fields) => {
    const decorationStats = this.props.message.decoration_stats;

    const decoratedFields = decorationStats ? Object.keys(decorationStats.changed_fields) : [];
    return Object.keys(fields)
      .sort()
      .map((key) => {
        return (
          <MessageField key={key}
                        {...this.props}
                        fieldName={key}
                        value={fields[key]}
                        disableFieldActions={this.props.disableFieldActions || decoratedFields.indexOf(key) !== -1} />
        );
      });
  };

  render() {
    const formattedFields = this.props.message.formatted_fields;
    const fields = this._formatFields(formattedFields);

    return (
      <dl className="message-details message-details-fields">
        {fields}
      </dl>
    );
  }
}

export default MessageFields;
