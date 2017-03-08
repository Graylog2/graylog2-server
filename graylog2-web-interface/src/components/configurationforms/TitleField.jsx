import React from 'react';

import { TextField } from 'components/configurationforms';

const TitleField = React.createClass({
  propTypes: {
    helpBlock: React.PropTypes.node,
    onChange: React.PropTypes.func,
    typeName: React.PropTypes.string.isRequired,
    value: React.PropTypes.any,
  },
  getDefaultProps() {
    return {
      helpBlock: <span />,
      onChange: () => {},
    };
  },

  render() {
    const typeName = this.props.typeName;
    const titleField = { is_optional: false, attributes: [], human_name: 'Title', description: this.props.helpBlock };
    return (
      <TextField key={`${typeName}-title`} typeName={typeName} title="title" field={titleField}
                                 value={this.props.value} onChange={this.props.onChange} autoFocus />
    );
  },
});

export default TitleField;
