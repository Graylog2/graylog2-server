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
import DocumentationLink from 'components/support/DocumentationLink';
import EditExtractor from 'components/extractors/EditExtractor';
import DocsHelper from 'util/DocsHelper';
import history from 'util/History';
import Routes from 'routing/Routes';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import withParams from 'routing/withParams';

const InputsActions = ActionsProvider.getActions('Inputs');
const ExtractorsActions = ActionsProvider.getActions('Extractors');
const ExtractorsStore = StoreProvider.getStore('Extractors');
const InputsStore = StoreProvider.getStore('Inputs');
const UniversalSearchstore = StoreProvider.getStore('UniversalSearch');

const EditExtractorsPage = createReactClass({
  displayName: 'EditExtractorsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(ExtractorsStore), Reflux.connect(InputsStore)],

  getInitialState() {
    return {
      extractor: undefined,
      exampleMessage: undefined,
    };
  },

  componentDidMount() {
    const { params } = this.props;

    InputsActions.get.triggerPromise(params.inputId);
    ExtractorsActions.get.triggerPromise(params.inputId, params.extractorId);

    UniversalSearchstore.search('relative', `gl2_source_input:${params.inputId} OR gl2_source_radio_input:${params.inputId}`, { relative: 3600 }, undefined, 1)
      .then((response) => {
        if (response.total_results > 0) {
          this.setState({ exampleMessage: response.messages[0] });
        } else {
          this.setState({ exampleMessage: {} });
        }
      });
  },

  _isLoading() {
    // eslint-disable-next-line react/destructuring-assignment
    return !(this.state.input && this.state.extractor && this.state.exampleMessage);
  },

  _extractorSaved() {
    let url;
    const { input } = this.state;
    const { params } = this.props;

    if (input.global) {
      url = Routes.global_input_extractors(params.inputId);
    } else {
      url = Routes.local_input_extractors(params.nodeId, params.inputId);
    }

    history.push(url);
  },

  render() {
    // TODO:
    // - Redirect when extractor or input were deleted

    if (this._isLoading()) {
      return <Spinner />;
    }

    const { extractor, exampleMessage, input } = this.state;

    return (
      <DocumentTitle title={`Edit extractor ${extractor.title}`}>
        <div>
          <PageHeader title={<span>Edit extractor <em>{extractor.title}</em> for input <em>{input.title}</em></span>}>
            <span>
              Extractors are applied on every message that is received by an input. Use them to extract and transform{' '}
              any text data into fields that allow you easy filtering and analysis later on.
            </span>

            <span>
              Find more information about extractors in the
              {' '}<DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation" />.
            </span>
          </PageHeader>
          <EditExtractor action="edit"
                         extractor={extractor}
                         inputId={input.id}
                         exampleMessage={exampleMessage.fields ? exampleMessage.fields[extractor.source_field] : undefined}
                         onSave={this._extractorSaved} />
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(EditExtractorsPage);
