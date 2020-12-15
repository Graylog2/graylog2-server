/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
