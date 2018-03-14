import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import EditExtractor from 'components/extractors/EditExtractor';

import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import history from 'util/History';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const ExtractorsStore = StoreProvider.getStore('Extractors');
const InputsStore = StoreProvider.getStore('Inputs');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');
const MessagesActions = ActionsProvider.getActions('Messages');

const CreateExtractorsPage = createReactClass({
  displayName: 'CreateExtractorsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(InputsStore)],

  getInitialState() {
    const { query } = this.props.location;

    return {
      extractor: ExtractorsStore.new(query.extractor_type, query.field),
      input: undefined,
      exampleMessage: undefined,
      extractorType: query.extractor_type,
      field: query.field,
      exampleIndex: query.example_index,
      exampleId: query.example_id,
    };
  },

  componentDidMount() {
    InputsActions.get.triggerPromise(this.props.params.inputId);
    MessagesActions.loadMessage.triggerPromise(this.state.exampleIndex, this.state.exampleId)
      .then(message => this.setState({ exampleMessage: message }));
  },

  _isLoading() {
    return !(this.state.input && this.state.exampleMessage);
  },

  _extractorSaved() {
    let url;
    if (this.state.input.global) {
      url = Routes.global_input_extractors(this.props.params.inputId);
    } else {
      url = Routes.local_input_extractors(this.props.params.nodeId, this.props.params.inputId);
    }

    history.push(url);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const exampleMessage = StringUtils.stringify(this.state.exampleMessage.fields[this.state.field]);

    return (
      <DocumentTitle title={`New extractor for input ${this.state.input.title}`}>
        <div>
          <PageHeader title={<span>New extractor for input <em>{this.state.input.title}</em></span>}>
            <span>
              Extractors are applied on every message that is received by an input. Use them to extract and
              transform any text data into fields that allow you easy filtering and analysis later on.
            </span>

            <span>
              Find more information about extractors in the
              {' '}<DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation" />.
            </span>
          </PageHeader>
          <EditExtractor action="create"
                         extractor={this.state.extractor}
                         inputId={this.state.input.id}
                         exampleMessage={exampleMessage}
                         onSave={this._extractorSaved} />
        </div>
      </DocumentTitle>
    );
  },
});

export default CreateExtractorsPage;
