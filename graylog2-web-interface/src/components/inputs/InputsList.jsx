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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled from 'styled-components';

import { Row, Col } from 'components/graylog';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import EntityList from 'components/common/EntityList';
import { IfPermitted, Spinner, SearchForm } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

import InputListItem from './InputListItem';
import CreateInputControl from './CreateInputControl';

const InputsActions = ActionsProvider.getActions('Inputs');
const SingleNodeActions = ActionsProvider.getActions('SingleNode');
const InputTypesActions = ActionsProvider.getActions('InputTypes');

const InputsStore = StoreProvider.getStore('Inputs');
const SingleNodeStore = StoreProvider.getStore('SingleNode');

const InputListRow = styled(Row)`
  h2 {
    margin-bottom: 5px;
  }

  .alert {
    margin-top: 10px;
  }

  .static-fields {
    margin-top: 10px;
    margin-left: 3px;

    ul {
      margin: 0;
      padding: 0;

      .remove-static-field {
        margin-left: 5px;
      }
    }
  }
`;

const InputsList = createReactClass({
  displayName: 'InputsList',

  propTypes: {
    permissions: PropTypes.array.isRequired,
    node: PropTypes.object,
  },

  mixins: [Reflux.connect(SingleNodeStore), Reflux.listenTo(InputsStore, '_splitInputs')],

  getDefaultProps() {
    return {
      node: undefined,
    };
  },

  getInitialState() {
    return {
      globalInputs: undefined,
      localInputs: undefined,
      filteredGlobalInputs: undefined,
      filteredLocalInputs: undefined,
      filter: undefined,
    };
  },

  componentDidMount() {
    InputTypesActions.list();
    InputsActions.list();
    SingleNodeActions.get();
  },

  _splitInputs(state) {
    const { inputs } = state;
    const globalInputs = inputs
      .filter((input) => input.global === true)
      .sort((inputA, inputB) => naturalSortIgnoreCase(inputA.title, inputB.title));
    let localInputs = inputs
      .filter((input) => input.global === false)
      .sort((inputA, inputB) => naturalSortIgnoreCase(inputA.title, inputB.title));

    if (this.props.node) {
      localInputs = localInputs.filter((input) => input.node === this.props.node.node_id);
    }

    this.setState({
      globalInputs: globalInputs,
      localInputs: localInputs,
    });

    this._onFilterInputs(this.state.filter);
  },

  _isLoading() {
    return !(this.state.localInputs && this.state.globalInputs && this.state.node && this.state.filteredLocalInputs
      && this.state.filteredGlobalInputs);
  },

  _formatInput(input) {
    return <InputListItem key={input.id} input={input} currentNode={this.state.node} permissions={this.props.permissions} />;
  },

  _nodeAffix() {
    return (this.props.node ? ' on this node' : '');
  },

  _onFilterInputs(filter, resetLoadingState) {
    const { globalInputs, localInputs } = this.state;
    const regExp = RegExp(filter, 'i');

    if (!globalInputs || !localInputs) {
      if (resetLoadingState) {
        resetLoadingState();
      }

      return;
    }

    if (!filter || filter.length <= 0) {
      this.setState({
        filteredGlobalInputs: globalInputs,
        filteredLocalInputs: localInputs,
        filter: undefined,
      });

      if (resetLoadingState) {
        resetLoadingState();
      }

      return;
    }

    const filterMethod = (input) => {
      return regExp.test(input.title);
    };

    const filteredGlobalInputs = this.state.globalInputs.filter(filterMethod);
    const filteredLocalInputs = this.state.localInputs.filter(filterMethod);

    this.setState({
      filteredGlobalInputs: filteredGlobalInputs,
      filteredLocalInputs: filteredLocalInputs,
      filter: filter,
    });

    if (resetLoadingState) {
      resetLoadingState();
    }
  },

  _onFilterReset() {
    const { globalInputs, localInputs } = this.state;

    this.setState({
      filteredGlobalInputs: globalInputs,
      filteredLocalInputs: localInputs,
      filter: undefined,
    });
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        {!this.props.node
        && (
        <IfPermitted permissions="inputs:create">
          <CreateInputControl />
        </IfPermitted>
        )}

        <InputListRow id="filter-input" className="content">
          <Col md={12}>
            <SearchForm onSearch={this._onFilterInputs}
                        topMargin={0}
                        onReset={this._onFilterReset}
                        searchButtonLabel="Filter"
                        placeholder="Filter by title" />
            <br />
            <h2>
              Global inputs
              &nbsp;
              <small>{this.state.globalInputs.length} configured{this._nodeAffix()}</small>
            </h2>
            <EntityList bsNoItemsStyle="info"
                        noItemsText={this.state.globalInputs.length <= 0 ? 'There are no global inputs.'
                          : 'No global inputs match the filter'}
                        items={this.state.filteredGlobalInputs.map((input) => this._formatInput(input))} />
            <br />
            <br />
            <h2>
              Local inputs
              &nbsp;
              <small>{this.state.localInputs.length} configured{this._nodeAffix()}</small>
            </h2>
            <EntityList bsNoItemsStyle="info"
                        noItemsText={this.state.localInputs.length <= 0 ? 'There are no local inputs.'
                          : 'No local inputs match the filter'}
                        items={this.state.filteredLocalInputs.map((input) => this._formatInput(input))} />
          </Col>
        </InputListRow>
      </div>
    );
  },
});

export default InputsList;
