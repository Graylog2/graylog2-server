import React from 'react';

import { ChangedMessageField, MessageField } from 'components/search';

const MessageFields = React.createClass({
  propTypes: {
    customFieldActions: React.PropTypes.node,
    disableFieldActions: React.PropTypes.bool,
    message: React.PropTypes.object.isRequired,
    possiblyHighlight: React.PropTypes.func.isRequired,
    showDecoration: React.PropTypes.bool,
  },

  getDefaultProps() {
    return {
      showDecoration: false,
    };
  },

  _formatFields(fields, showDecoration) {
    const decorationStats = this.props.message.decoration_stats;

    if (!showDecoration || !decorationStats) {
      const decoratedFields = decorationStats ? Object.keys(decorationStats.changed_fields) : [];
      return Object.keys(fields)
        .sort()
        .map((key) => {
          return (
            <MessageField key={key} {...this.props} fieldName={key} value={fields[key]}
                               disableFieldActions={this.props.disableFieldActions || decoratedFields.indexOf(key) !== -1} />
          );
        });
    }

    const allKeys = Object.keys(decorationStats.removed_fields).concat(Object.keys(fields)).sort();

    return allKeys.map((key) => {
      if (decorationStats.added_fields[key]) {
        return <ChangedMessageField key={key} fieldName={key} newValue={fields[key]} />;
      }
      if (decorationStats.changed_fields[key]) {
        return (<ChangedMessageField key={key}
                                     fieldName={key}
                                     originalValue={decorationStats.changed_fields[key]}
                                     newValue={fields[key]} />);
      }

      if (decorationStats.removed_fields[key]) {
        return (<ChangedMessageField key={key}
                                     fieldName={key}
                                     originalValue={decorationStats.removed_fields[key]} />);
      }

      return <MessageField key={key} {...this.props} fieldName={key} value={fields[key]} disableFieldActions />;
    });
  },
  render() {
    const formattedFields = this.props.message.formatted_fields;
    const fields = this._formatFields(formattedFields, this.props.showDecoration);

    return (
      <dl className="message-details message-details-fields">
        {fields}
      </dl>
    );
  },
});

export default MessageFields;
