import React from 'react';

import { MessageFieldDescription } from 'components/search';

const MessageField = React.createClass({
  propTypes: {
    customFieldActions: React.PropTypes.node,
    disableFieldActions: React.PropTypes.bool,
    fieldName: React.PropTypes.string.isRequired,
    message: React.PropTypes.object.isRequired,
    possiblyHighlight: React.PropTypes.func.isRequired,
    value: React.PropTypes.any.isRequired,
  },
  SPECIAL_FIELDS: ['full_message', 'level'],
  _isAdded(key) {
    const decorationStats = this.props.message.decoration_stats;
    return decorationStats && decorationStats.added_fields && decorationStats.added_fields[key] !== undefined;
  },
  _isChanged(key) {
    const decorationStats = this.props.message.decoration_stats;
    return decorationStats && decorationStats.changed_fields && decorationStats.changed_fields[key] !== undefined;
  },
  _isDecorated(key) {
    return this._isAdded(key) || this._isChanged(key);
  },
  render() {
    let innerValue = this.props.value;
    const key = this.props.fieldName;
    if (this.SPECIAL_FIELDS.indexOf(key) !== -1) {
      innerValue = this.props.message.fields[key];
    }

    return (
      <span>
        <dt key={`${key}Title`}>{key}</dt>
        <MessageFieldDescription key={`${key}Description`}
                                 message={this.props.message}
                                 fieldName={key}
                                 fieldValue={innerValue}
                                 possiblyHighlight={this.props.possiblyHighlight}
                                 disableFieldActions={this._isAdded(key) || this.props.disableFieldActions}
                                 customFieldActions={this.props.customFieldActions}
                                 isDecorated={this._isDecorated(key)} />
      </span>
    );
  },
});

export default MessageField;
