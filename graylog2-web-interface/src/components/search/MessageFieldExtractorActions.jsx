import React, { PropTypes } from 'react';
import { DropdownButton, MenuItem } from 'react-bootstrap';
import ExtractorUtils from 'util/ExtractorUtils';

const MessageFieldExtractorActions = React.createClass({
  propTypes: {
    fieldName: PropTypes.string.isRequired,
    message: PropTypes.object.isRequired,
  },
  componentWillMount() {
    this._refreshExtractorRoutes(this.props);
  },
  componentWillReceiveProps(nextProps) {
    this._refreshExtractorRoutes(nextProps);
  },
  _refreshExtractorRoutes(props) {
    this.newExtractorRoutes = ExtractorUtils.getNewExtractorRoutes(props.message.source_node_id,
      props.message.source_input_id, props.fieldName, props.message.index, props.message.id);
  },
  _formatExtractorMenuItem(extractorType) {
    return (
      <MenuItem key={`menu-item-${extractorType}`} href={this.newExtractorRoutes[extractorType]}>
        {ExtractorUtils.getReadableExtractorTypeName(extractorType)}
      </MenuItem>
    );
  },
  render() {
    const messageField = this.props.message.fields[this.props.fieldName];
    if (typeof messageField === 'string') {
      return (
        <div className="message-field-actions pull-right">
          <DropdownButton pullRight
                            bsSize="xsmall"
                            title="Select extractor type"
                            key={1}
                            id={`select-extractor-type-dropdown-field-${this.props.fieldName}`}>
            {ExtractorUtils.EXTRACTOR_TYPES.map(extractorType => this._formatExtractorMenuItem(extractorType))}
          </DropdownButton>
        </div>
      );
    }
    return (
      <div className="message-field-actions pull-right">
        <DropdownButton pullRight
                              bsSize="xsmall"
                              title="Select extractor type"
                              key={1}
                              id={`select-extractor-type-dropdown-field-${this.props.fieldName}`}>
          <MenuItem key="select-extractor-type-disabled" disabled>
                    Extractors can only be used with string fields.
                </MenuItem>
        </DropdownButton>
      </div>
    );
  },
});

export default MessageFieldExtractorActions;
