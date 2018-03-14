import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Link } from 'react-router';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const InputStatesStore = StoreProvider.getStore('InputStates');

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { InputsList } from 'components/inputs';

import Routes from 'routing/Routes';

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

const NodeInputsPage = createReactClass({
  displayName: 'NodeInputsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore), Reflux.connectFilter(NodesStore, 'node', nodeFilter)],

  componentDidMount() {
    this.interval = setInterval(InputStatesStore.list, 2000);
  },

  componentWillUnmount() {
    clearInterval(this.interval);
  },

  _isLoading() {
    return !this.state.node;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const title = <span>Inputs of node {this.state.node.short_node_id} / {this.state.node.hostname}</span>;

    return (
      <DocumentTitle title={`Inputs of node ${this.state.node.short_node_id} / ${this.state.node.hostname}`}>
        <div>
          <PageHeader title={title}>
            <span>Graylog nodes accept data via inputs. On this page you can see which inputs are running on this specific node.</span>

            <span>
              You can launch and terminate inputs on your cluster <Link to={Routes.SYSTEM.INPUTS}>here</Link>.
            </span>
          </PageHeader>
          <InputsList permissions={this.state.currentUser.permissions} node={this.state.node} />
        </div>
      </DocumentTitle>
    );
  },
});

export default NodeInputsPage;
