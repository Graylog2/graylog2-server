import PropTypes from 'prop-types';
import React from 'react';

import ExtractorUtils from 'util/ExtractorUtils';
import { SplitButton, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';

class MessageFieldSearchActions extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    message: PropTypes.object.isRequired,
    onLoadTerms: PropTypes.func.isRequired,
    onAddFieldToSearchBar: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);
    this.newExtractorRoutes = ExtractorUtils.getNewExtractorRoutes(props.message.source_node_id,
      props.message.source_input_id, props.fieldName, props.message.index, props.message.id);

    this.state = null;
  }

  _formatExtractorMenuItem = (extractorType) => {
    return (
      <MenuItem key={`menu-item-${extractorType}`} href={this.newExtractorRoutes[extractorType]}>
        {ExtractorUtils.getReadableExtractorTypeName(extractorType)}
      </MenuItem>
    );
  };

  render() {
    const messageField = this.props.message.fields[this.props.fieldName];
    let extractors;
    if (typeof messageField === 'string') {
      extractors = (
        <li className="dropdown-submenu left-submenu">
          <a href="#">Create extractor for field {this.props.fieldName}</a>
          <ul className="dropdown-menu">
            {ExtractorUtils.EXTRACTOR_TYPES.map(extractorType => this._formatExtractorMenuItem(extractorType))}
          </ul>
        </li>
      );
    } else {
      extractors = (<MenuItem disabled>Extractors can only be used with string fields</MenuItem>);
    }
    return (
      <div className="message-field-actions pull-right">
        <SplitButton pullRight
                     bsSize="xsmall"
                     title={<Icon name="search-plus" />}
                     key={1}
                     onClick={this.props.onAddFieldToSearchBar}
                     id={`more-actions-dropdown-field-${this.props.fieldName}`}>
          {extractors}
          <MenuItem onSelect={this.props.onLoadTerms(this.props.fieldName)}>Show terms
            of <em>{this.props.fieldName}</em>
          </MenuItem>
        </SplitButton>
      </div>
    );
  }
}

export default MessageFieldSearchActions;
