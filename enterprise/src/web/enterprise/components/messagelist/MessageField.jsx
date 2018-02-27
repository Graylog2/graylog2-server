import PropTypes from 'prop-types';
import React from 'react';

import { MessageFieldDescription } from 'components/search';
import Field from '../Field';
import Value from '../Value';

const MessageField = React.createClass({
  propTypes: {
    customFieldActions: PropTypes.node,
    disableFieldActions: PropTypes.bool,
    fieldName: PropTypes.string.isRequired,
    message: PropTypes.object.isRequired,
    possiblyHighlight: PropTypes.func.isRequired,
    value: PropTypes.any.isRequired,
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
    const key = this.props.fieldName;
    let innerValue = <Value queryId="FIXME" field={key} value={this.props.value}>{this.props.value}</Value>;
    if (this.SPECIAL_FIELDS.indexOf(key) !== -1) {
      innerValue = <Value queryId="FIXME" field={key} value={this.props.message.fields[key]}>{this.props.message.fields[key]}</Value>;
    }

    return (
      <span>
        <dt key={`${key}Title`}><Field interactive queryId="FIXME" name={key}>{key}</Field></dt>
        <dd>{innerValue}</dd>
      </span>
    );
  },
});

export default MessageField;
