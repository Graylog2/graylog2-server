import React from 'react';

import { Spinner } from 'components/common';
import ExtendedSearchPage from './ExtendedSearchPage';
import { ViewActions } from './stores/ViewStore';

export default class NewSearchPage extends React.Component {
  static propTypes = {};

  constructor(props, context) {
    super(props, context);
    this.state = {
      loaded: false,
    };
  }

  componentDidMount() {
    ViewActions.create().then(() => this.setState({ loaded: true }));
  }

  render() {
    if (this.state.loaded) {
      return <ExtendedSearchPage />;
    }
    return <Spinner />;
  }
}
