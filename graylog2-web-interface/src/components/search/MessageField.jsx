import React from 'react';

import { DecoratedMessageFieldMarker, MessageFieldDescription } from 'components/search';

const MessageField = React.createClass({
  propTypes: {
    customFieldActions: React.PropTypes.node,
    disableFieldActions: React.PropTypes.bool,
    fieldName: React.PropTypes.string.isRequired,
    message: React.PropTypes.object.isRequired,
    possiblyHighlight: React.PropTypes.func.isRequired,
    value: React.PropTypes.stirng.isRequired,
  },
  getInitialState() {
    return {
      showOriginal: false,
    };
  },

  SPECIAL_FIELDS: ['full_message', 'level'],
  _decorationMarker(key) {
    if (this._isAdded(key)) {
      return <DecoratedMessageFieldMarker title="This field was added by a decorator. It was not present in the original message, so you cannot search for it." />;
    }

    if (this._isChanged(key)) {
      return (<DecoratedMessageFieldMarker title="This field was modified by a decorator. Click here to show the original content."
                                           onClick={this._toggleShowOriginalContent} />);
    }

    return null;
  },
  _isAdded(key) {
    const decorationStats = this.props.message.decoration_stats;
    return decorationStats && decorationStats.added_fields && decorationStats.added_fields[key] !== undefined;
  },
  _isChanged(key) {
    const decorationStats = this.props.message.decoration_stats;
    return decorationStats && decorationStats.changed_fields && decorationStats.changed_fields[key] !== undefined;
  },
  _originalValue(key) {
    const decorationStats = this.props.message.decoration_stats;
    if (decorationStats && decorationStats.changed_fields) {
      return decorationStats.changed_fields[key];
    }

    return null;
  },
  _toggleShowOriginalContent() {
    this.setState({ showOriginal: !this.state.showOriginal });
  },
  _wrapPossiblyHighlight(fieldName) {
    if (this.state.showOriginal) {
      return <span>{this._originalValue(fieldName)} <i>(Original Content)</i></span>;
    } else {
      return this.props.possiblyHighlight(fieldName);
    }
  },
  render() {
    let innerValue = this.props.value;
    const key = this.props.fieldName;
    if (this.SPECIAL_FIELDS.indexOf(key) !== -1) {
      innerValue = this.props.message.fields[key];
    }

    if (this.state.showOriginal) {
      innerValue = this._originalValue(key);
    }

    return (
      <span>
        <dt key={`${key}Title`}>{key} {this._decorationMarker(key)}</dt>
        <MessageFieldDescription key={`${key}Description`}
                                 message={this.props.message}
                                 fieldName={key}
                                 fieldValue={innerValue}
                                 possiblyHighlight={this._wrapPossiblyHighlight}
                                 disableFieldActions={this._isAdded(key) || this.props.disableFieldActions}
                                 customFieldActions={this.props.customFieldActions}/>
      </span>
    );
  },
});

export default MessageField;
