// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import Spinner from 'components/common/Spinner';

import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import withPluginEntities from 'views/logic/withPluginEntities';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import type { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import ViewLoader from 'views/logic/views/ViewLoader';
import { SearchActions } from 'views/stores/SearchStore';

import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  executingViewHooks: Array<ViewHook>,
  loadingViewHooks: Array<ViewHook>,
  location: {
    query: { [string]: any },
  },
  params: {
    viewId: string,
  },
  route: any,
  viewLoader: ViewLoaderFn,
};

type State = {
  hookComponent: ?any,
  loaded: boolean,
};


class ShowViewPage extends React.Component<Props, State> {
  static propTypes = {
    location: PropTypes.shape({
      query: PropTypes.object,
    }).isRequired,
    params: PropTypes.shape({
      viewId: PropTypes.string.isRequired,
    }).isRequired,
    route: PropTypes.object.isRequired,
    viewLoader: PropTypes.func,
  };

  static defaultProps = {
    viewLoader: ViewLoader,
  };

  state = {
    hookComponent: undefined,
    loaded: false,
  };

  componentDidMount = () => {
    const { params } = this.props;
    const { viewId } = params;

    return this.loadView(viewId);
  };

  loadView = (viewId: string): Promise<?View> => {
    const { location, loadingViewHooks, executingViewHooks, viewLoader } = this.props;
    const { query } = location;

    return viewLoader(
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
    ).then((results) => {
      this.setState({ loaded: true });
      return results;
    }).then(() => SearchActions.executeWithCurrentState())
      SearchActions.executeWithCurrentState();
    }).catch(e => e);
  };

  render() {
    const { hookComponent, loaded } = this.state;
    if (hookComponent) {
      const HookComponent = hookComponent;
      return <HookComponent />;
    }

    if (!loaded) {
      return <Spinner />;
    }

    const { route } = this.props;

    return (
      <ViewLoaderContext.Provider value={this.loadView}>
        <ExtendedSearchPage route={route} />
      </ViewLoaderContext.Provider>
    );
  }
}

const ShowViewPageWithStores = connect(ShowViewPage, { executionState: SearchExecutionStateStore });
const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(ShowViewPageWithStores, mapping);
