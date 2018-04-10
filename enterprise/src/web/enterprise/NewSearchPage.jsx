import React from 'react';

import ExtendedSearchPage from './ExtendedSearchPage';
import ViewsActions from './actions/ViewsActions';
import ViewsStore from './stores/ViewsStore';
import { Spinner } from 'components/common';

export default class NewSearchPage extends React.Component {
  static propTypes = {};

  constructor(props, context) {
    super(props, context);
    this.state = {
      loaded: false,
    };
  }

  componentDidMount() {
    ViewsActions.create().then(() => this.setState({ loaded: true }));
  }

  render() {
    if (this.state.loaded) {
      return <ExtendedSearchPage />;
    }
    return <Spinner />;
  }
};
