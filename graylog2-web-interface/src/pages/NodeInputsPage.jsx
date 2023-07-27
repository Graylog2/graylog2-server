/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Link } from 'components/common/router';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { InputsList } from 'components/inputs';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import useParams from "routing/useParams";
import useCurrentUser from "../hooks/useCurrentUser";
import {useStore} from "stores/connect";

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

const NodeInputsPage = () => {
  const { nodeId } = useParams();

  const currentUser = useCurrentUser();
  const {nodes} = useStore(NodesStore)
  const node = nodes?.[nodeId];

  useEffect(() => {
    const interval = setInterval(InputStatesStore.list, 2000);

    return () => {
      clearInterval(this.interval);
    }
  }, [])

    if (!node) {
      return <Spinner />;
    }

    const title = <span>Inputs of node {this.state.node.short_node_id} / {this.state.node.hostname}</span>;

    return (
      <DocumentTitle title={`Inputs of node ${this.state.node.short_node_id} / ${this.state.node.hostname}`}>
        <div>
          <PageHeader title={title}>
            <span>
              Graylog nodes accept data via inputs. On this page you can see which inputs are running on this specific node.<br />
              You can launch and terminate inputs on your cluster <Link to={Routes.SYSTEM.INPUTS}>here</Link>.
            </span>
          </PageHeader>
          <InputsList permissions={currentUser.permissions} node={node} />
        </div>
      </DocumentTitle>
    );
};

export default withParams(NodeInputsPage);
