import React, { PropTypes } from 'react';
import Reflux from 'reflux';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import EditExtractor from 'components/extractors/EditExtractor';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');
const ExtractorsActions = ActionsProvider.getActions('Extractors');

import StoreProvider from 'injection/StoreProvider';
const ExtractorsStore = StoreProvider.getStore('Extractors');
const InputsStore = StoreProvider.getStore('Inputs');
const UniversalSearchstore = StoreProvider.getStore('UniversalSearch');

const EditExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(ExtractorsStore), Reflux.connect(InputsStore)],
  getInitialState() {
    return {
      extractor: undefined,
      input: undefined,
      exampleMessage: undefined,
    };
  },
  componentDidMount() {
    InputsActions.get.triggerPromise(this.props.params.inputId);
    ExtractorsActions.get.triggerPromise(this.props.params.inputId, this.props.params.extractorId);
    UniversalSearchstore.search('relative', `gl2_source_input:${this.props.params.inputId} OR gl2_source_radio_input:${this.props.params.inputId}`, { range: 0 }, undefined, 1)
      .then((response) => {
        if (response.total_results > 0) {
          this.setState({ exampleMessage: response.messages[0] });
        } else {
          this.setState({ exampleMessage: {} });
        }
      });
  },
  _isLoading() {
    return !(this.state.input && this.state.extractor && this.state.exampleMessage);
  },
  _extractorSaved() {
    let url;
    if (this.state.input.global) {
      url = Routes.global_input_extractors(this.props.params.inputId);
    } else {
      url = Routes.local_input_extractors(this.props.params.nodeId, this.props.params.inputId);
    }

    this.props.history.pushState(null, url);
  },
  render() {
    // TODO:
    // - Redirect when extractor or input were deleted

    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title={`Edit extractor ${this.state.extractor.title}`}>
        <div>
          <PageHeader
            title={<span>Edit extractor <em>{this.state.extractor.title}</em> for input <em>{this.state.input.title}</em></span>}>
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
                         extractor={this.state.extractor}
                         inputId={this.state.input.id}
                         exampleMessage={this.state.exampleMessage.fields ? this.state.exampleMessage.fields[this.state.extractor.source_field] : undefined}
                         onSave={this._extractorSaved} />
        </div>
      </DocumentTitle>
    );
  },
});

export default EditExtractorsPage;
