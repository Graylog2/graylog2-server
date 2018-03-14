import React from 'react';

import Select from 'components/common/Select';

/**
 * Component that wraps and render a `Select` where multiple options can be selected. It passes all
 * props to the underlying `Select` component, so please look there to find more information about them.
 */
class MultiSelect extends React.Component {
  static propTypes = Select.propTypes;
  _select = undefined;

  getValue = () => {
    return this._select.getValue();
  };

  render() {
    return <Select ref={(c) => { this._select = c; }} multi {...this.props} />;
  }
}

export default MultiSelect;
