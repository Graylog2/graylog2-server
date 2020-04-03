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

import ActionsProvider from 'injection/ActionsProvider';

const ExtractorsStore = StoreProvider.getStore('Extractors');
const InputsStore = StoreProvider.getStore('Inputs');
// eslint-disable-next-line no-unused-vars
const MessagesStore = StoreProvider.getStore('Messages');
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
    const { params } = this.props;
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
          <PageHeader title={<span>New extractor for input <em>{input.title}</em></span>}>
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
                         extractor={extractor}
                         inputId={input.id}
                         exampleMessage={stringifiedExampleMessage}
                         onSave={this._extractorSaved} />
        </div>
      </DocumentTitle>
    );
  },
});

export default CreateExtractorsPage;
