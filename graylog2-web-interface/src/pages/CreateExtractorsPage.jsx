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

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import EditExtractor from 'components/extractors/EditExtractor';
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';
import { ExtractorsStore } from 'stores/extractors/ExtractorsStore';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import { MessagesActions } from 'stores/messages/MessagesStore';
import withHistory from 'routing/withHistory';

const CreateExtractorsPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'CreateExtractorsPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    history: PropTypes.object.isRequired,
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(InputsStore)],

  getInitialState() {
    const { location } = this.props;
    const { query } = location;

    return {
      extractor: ExtractorsStore.new(query.extractor_type, query.field),
      exampleMessage: undefined,
      field: query.field,
      exampleIndex: query.example_index,
      exampleId: query.example_id,
    };
  },

  componentDidMount() {
    const { params } = this.props;

    InputsActions.get.triggerPromise(params.inputId);
    const { exampleIndex, exampleId } = this.state;

    MessagesActions.loadMessage.triggerPromise(exampleIndex, exampleId)
      .then((message) => this.setState({ exampleMessage: message }));
  },

  _isLoading() {
    const { exampleMessage, input } = this.state;

    return !(input && exampleMessage);
  },

  _extractorSaved() {
    let url;
    const { params, history } = this.props;
    const { input } = this.state;

    if (input.global) {
      url = Routes.global_input_extractors(params.inputId);
    } else {
      url = Routes.local_input_extractors(params.nodeId, params.inputId);
    }

    history.push(url);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { field, exampleMessage, extractor, input } = this.state;
    const stringifiedExampleMessage = StringUtils.stringify(exampleMessage.fields[field]);

    return (
      <DocumentTitle title={`New extractor for input ${input.title}`}>
        <div>
          <PageHeader title={<span>New extractor for input <em>{input.title}</em></span>}
                      documentationLink={{
                        title: 'Extractors documentation',
                        path: DocsHelper.PAGES.EXTRACTORS,
                      }}>
            <span>
              Extractors are applied on every message that is received by an input. Use them to extract and
              transform any text data into fields that allow you easy filtering and analysis later on.
            </span>
          </PageHeader>
          <EditExtractor action="create"
                         extractor={extractor}
                         inputId={input.id}
                         exampleMessage={stringifiedExampleMessage}
                         onSave={this._extractorSaved} />
        </div>
      </DocumentTitle>
    );
  },
});

export default withHistory(withParams(withLocation(CreateExtractorsPage)));
