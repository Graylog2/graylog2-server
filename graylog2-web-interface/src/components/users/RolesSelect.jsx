import PropTypes from 'prop-types';
import React from 'react';

import MultiSelect from 'components/common/MultiSelect';

const RolesSelect = React.createClass({
  propTypes: {
    userRoles: PropTypes.arrayOf(PropTypes.string),
    availableRoles: PropTypes.array.isRequired,
    onValueChange: PropTypes.func,
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
