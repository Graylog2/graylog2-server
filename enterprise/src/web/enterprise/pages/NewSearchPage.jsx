import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import { ViewActions } from 'enterprise/stores/ViewStore';
import ExtendedSearchPage from './ExtendedSearchPage';

export default class NewSearchPage extends React.Component {
  static propTypes = {
    route: PropTypes.object.isRequired,
  };

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
      return <ExtendedSearchPage route={this.props.route} />;
    }
    return <Spinner />;
  }
}
