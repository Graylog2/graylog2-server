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

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ExportExtractors from 'components/extractors/ExportExtractors';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import withParams from 'routing/withParams';

const InputsActions = ActionsProvider.getActions('Inputs');
const InputsStore = StoreProvider.getStore('Inputs');

const ExportExtractorsPage = createReactClass({
  displayName: 'ExportExtractorsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(InputsStore)],

  componentDidMount() {
    const { params } = this.props;

    InputsActions.get.triggerPromise(params.inputId);
  },

  _isLoading() {
    // eslint-disable-next-line react/destructuring-assignment
    return !this.state.input;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { input } = this.state;

    return (
      <DocumentTitle title={`Export extractors of ${input.title}`}>
        <div>
          <PageHeader title={<span>Export extractors of <em>{input.title}</em></span>}>
            <span>
              The extractors of an input can be exported to JSON for importing into other setups
              or sharing in <a href="https://marketplace.graylog.org/" rel="noopener noreferrer" target="_blank">the Graylog Marketplace</a>.
            </span>
          </PageHeader>
          <ExportExtractors input={input} />
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(ExportExtractorsPage);
