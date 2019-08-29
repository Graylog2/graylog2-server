// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import withPluginEntities from 'views/logic/withPluginEntities';
import { Spinner } from 'components/common';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import View from 'views/logic/views/View';
import ViewLoader from 'views/logic/views/ViewLoader';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import { SearchActions } from 'views/stores/SearchStore';

import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  route: {},
  location: {
    query: { [string]: any },
  },
  executingViewHooks: Array<ViewHook>,
  loadingViewHooks: Array<ViewHook>,
  viewStoreState: ViewStoreState,
};

type State = {
  loaded: boolean,
  hookComponent: ?any,
  loadedView: ?View,
};

class NewSearchPage extends React.Component<Props, State> {
  static propTypes = {
    route: PropTypes.object.isRequired,
    location: PropTypes.shape({
      query: PropTypes.object,
    }).isRequired,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      hookComponent: undefined,
      loaded: false,
      loadedView: undefined,
    };
  }

  componentDidMount() {
    ViewActions.create().then(() => this.setState({ loaded: true }));
  }

  loadView = (viewId: string): Promise<?View> => {
    const { location, loadingViewHooks, executingViewHooks } = this.props;
    const { query } = location;

    return ViewLoader(
      viewId,
      loadingViewHooks,
      executingViewHooks,
      query,
      () => {
        this.setState({ hookComponent: undefined });
      },
      (e) => {
        if (e instanceof Error) {
          throw e;
        }
        this.setState({ hookComponent: e });
      },
    ).then((view) => {
      this.setState({ loaded: true, loadedView: view });
      return view;
    }).then(() => {
      SearchActions.executeWithCurrentState();
    }).catch(e => e);
  };

  render() {
    const { hookComponent, loaded, loadedView } = this.state;
    const { viewStoreState } = this.props;
    const { dirty } = viewStoreState;

    if (hookComponent) {
      const HookComponent = hookComponent;
      return <HookComponent />;
    }

    if (loaded) {
      const { route } = this.props;
      return (
        <ViewLoaderContext.Provider value={{ loaderFunc: this.loadView, dirty, loadedView }}>
          <ViewTypeContext.Provider value={View.Type.Search}>
            <ExtendedSearchPage route={route} />
          </ViewTypeContext.Provider>
        </ViewLoaderContext.Provider>
      );
    }
    return <Spinner />;
  }
}

const NewSearchPageWithStores = connect(NewSearchPage, { executionState: SearchExecutionStateStore, viewStoreState: ViewStore });
const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(NewSearchPageWithStores, mapping);
