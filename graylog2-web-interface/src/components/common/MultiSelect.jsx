import React from 'react';

import Select from 'components/common/Select';

const MultiSelect = React.createClass({
  propTypes: Select.propTypes,
  _select: undefined,
  getValue() {
    return this._select.getValue();
  },
  render() {
    return <Select ref={(c) => { this._select = c; }} multi {...this.props} />;
  },
});

export default MultiSelect;
