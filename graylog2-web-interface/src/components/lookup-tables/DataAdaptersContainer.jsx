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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersActions, LookupTableDataAdaptersStore } = CombinedProvider.get(
  'LookupTableDataAdapters',
);

const DataAdaptersContainer = createReactClass({
  displayName: 'DataAdaptersContainer',

  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node,
    ]),
  },

  mixins: [Reflux.connect(LookupTableDataAdaptersStore)],

  getDefaultProps() {
    return {
      children: null,
    };
  },

  componentDidMount() {
    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    LookupTableDataAdaptersActions.searchPaginated(1, 10000, null);
  },

  render() {
    if (!this.state.dataAdapters) {
      return <Spinner />;
    }

    const childrenWithProps = React.Children.map(this.props.children,
      (child) => React.cloneElement(child,
        { dataAdapters: this.state.dataAdapters, pagination: this.state.pagination }));

    return <div>{childrenWithProps}</div>;
  },
});

export default DataAdaptersContainer;
