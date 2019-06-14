// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import { ViewActions } from 'views/stores/ViewStore';
import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  route: {}
};

type State = {
  loaded: boolean,
};

export default class NewSearchPage extends React.Component<Props, State> {
  static propTypes = {
    route: PropTypes.object.isRequired,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      loaded: false,
    };
  }

  componentDidMount() {
    ViewActions.create().then(() => this.setState({ loaded: true }));
  }

  render() {
    const { loaded } = this.state;
    if (loaded) {
      const { route } = this.props;
      return <ExtendedSearchPage route={route} />;
    }
    return <Spinner />;
  }
}
