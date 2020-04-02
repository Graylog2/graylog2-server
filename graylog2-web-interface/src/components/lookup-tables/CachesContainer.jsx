import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableCachesActions, LookupTableCachesStore } = CombinedProvider.get(
  'LookupTableCaches',
);

const CachesContainer = createReactClass({
  displayName: 'CachesContainer',

  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node,
    ]),
  },

  mixins: [Reflux.connect(LookupTableCachesStore)],

  getDefaultProps() {
    return {
      children: null,
    };
  },

  componentDidMount() {
    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    LookupTableCachesActions.searchPaginated(1, 10000, null);
  },

  render() {
    if (!this.state.caches) {
      return <Spinner />;
    }
    const childrenWithProps = React.Children.map(this.props.children,
      (child) => React.cloneElement(child,
        { caches: this.state.caches, pagination: this.state.pagination }));
    return <div>{childrenWithProps}</div>;
  },
});

export default CachesContainer;
