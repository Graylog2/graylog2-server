import React from 'react';

import MultiSelect from 'components/common/MultiSelect';

const RolesSelect = React.createClass({
  propTypes: {
    userRoles: React.PropTypes.arrayOf(React.PropTypes.string),
    availableRoles: React.PropTypes.array.isRequired,
    onValueChange: React.PropTypes.func,
  },
  getDefaultProps() {
    return {
      userRoles: [],
    };
  },
  getValue() {
    return this.refs.select.getValue().split(',');
  },
  render() {
    const rolesValue = this.props.userRoles.join(',');
    const rolesOptions = this.props.availableRoles.map((role) => {
      return { value: role.name, label: role.name };
    });
    return (
      <MultiSelect
        ref="select"
        options={rolesOptions}
        value={rolesValue}
        onValueChange={this.props.onValueChange}
        placeholder="Choose roles..."
      />
    );
  },
});

export default RolesSelect;
