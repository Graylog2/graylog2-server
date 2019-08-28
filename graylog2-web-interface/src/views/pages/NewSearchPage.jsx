// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
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
      return (
        <ViewTypeContext.Provider value={View.Type.Search}>
          <ExtendedSearchPage route={route} />
        </ViewTypeContext.Provider>
      );
    }
    return <Spinner />;
  }
}
