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
