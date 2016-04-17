import React from 'react';

import Select from 'components/common/Select';

const MultiSelect = React.createClass({
  propTypes: Select.propTypes,
  getValue() {
    return this.refs.select.getValue();
  },
  render() {
    return <Select ref="select" multi {...this.props} />;
  },
});

export default MultiSelect;
