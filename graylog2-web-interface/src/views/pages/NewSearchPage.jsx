// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import UserNotification from 'util/UserNotification';
import withPluginEntities from 'views/logic/withPluginEntities';
import { Spinner } from 'components/common';
import { ViewActions } from 'views/stores/ViewStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import View from 'views/logic/views/View';
import ViewLoader, { processHooks } from 'views/logic/views/ViewLoader';
import { SearchActions } from 'views/stores/SearchStore';
import { ExtendedSearchPage } from 'views/pages';
import { syncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';

type URLQuery = { [string]: any }

type Props = {
  route: {},
  router: {
    getCurrentLocation: () => ({ pathname: string, search: string }),
  },
  location: {
    query: URLQuery,
    pathname: string
  },
  executingViewHooks: Array<ViewHook>,
  loadingViewHooks: Array<ViewHook>,
};

type State = {
  loaded: boolean,
  hookComponent: ?any,
};

class NewSearchPage extends React.Component<Props, State> {
  static propTypes = {
    route: PropTypes.object.isRequired,
    router: PropTypes.object.isRequired,
    location: PropTypes.shape({
      query: PropTypes.object,
      pathname: PropTypes.string,
    }).isRequired,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      hookComponent: undefined,
      loaded: false,
    };
  }

  componentDidMount() {
    this.loadViewFromParams();
  }

  loadViewFromParams = (): Promise<?View> => {
    const { location: { query } } = this.props;
    return this.loadNewView({ ...query });
  }

  loadEmptyView = (): Promise<?View> => {
    const { router } = this.props;
    return this.loadNewView({}).then(() => {
      const { pathname, search } = router.getCurrentLocation();
      const query = `${pathname}${search}`;
      syncWithQueryParameters(query);
    });
  }

  loadNewView = (currentURLQuery: URLQuery): Promise<?View> => {
    const { loadingViewHooks, executingViewHooks } = this.props;
    return processHooks(
      ViewActions.create(View.Type.Search).then(({ view }) => view),
      loadingViewHooks,
      executingViewHooks,
      currentURLQuery,
    ).then(
      () => this.setState({ loaded: true }),
    ).catch(
      error => UserNotification.error(`Executing search failed with error: ${error}`, 'Could not execute search'),
    );
  };

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
      this.setState({ loaded: true });
      return view;
    }).then(() => {
      SearchActions.executeWithCurrentState();
    }).catch(e => e);
  };

  render() {
    const { hookComponent, loaded } = this.state;

    if (hookComponent) {
      const HookComponent = hookComponent;
      return <HookComponent />;
    }

    if (loaded) {
      const { location, route } = this.props;
      return (
        <ViewLoaderContext.Provider value={this.loadView}>
          <NewViewLoaderContext.Provider value={this.loadEmptyView}>
            <ExtendedSearchPage route={route} location={location} />
          </NewViewLoaderContext.Provider>
        </ViewLoaderContext.Provider>
      );
    }
    return <Spinner />;
  }
}

const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(withRouter(NewSearchPage), mapping);
