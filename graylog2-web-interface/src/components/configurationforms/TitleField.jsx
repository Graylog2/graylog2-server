import PropTypes from 'prop-types';
import React from 'react';

import { TextField } from 'components/configurationforms';

class TitleField extends React.Component {
  static propTypes = {
    helpBlock: PropTypes.node,
    onChange: PropTypes.func,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
  };

  static defaultProps = {
    helpBlock: <span />,
    onChange: () => {},
  };

  render() {
    const typeName = this.props.typeName;
    const titleField = { is_optional: false, attributes: [], human_name: 'Title', description: this.props.helpBlock };
    return (
      <TextField key={`${typeName}-title`} typeName={typeName} title="title" field={titleField}
                                 value={this.props.value} onChange={this.props.onChange} autoFocus />
    );
  }
}

export default TitleField;
