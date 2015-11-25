import React, {PropTypes} from 'react';

import Spinner from 'components/common/Spinner';
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import EditExtractor from 'components/extractors/EditExtractor';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import ExtractorsStore from 'stores/extractors/ExtractorsStore';
import InputsStore from 'stores/inputs/InputsStore';
import MessagesStore from 'stores/messages/MessagesStore';

const CreateExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  },
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
    InputsStore.get(this.props.params.inputId).then(input => this.setState({input: input}));
    MessagesStore.loadMessage(this.state.exampleIndex, this.state.exampleId)
      .then(message => this.setState({exampleMessage: message}));
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

    this.props.history.pushState(null, url);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <div>
        <PageHeader title={<span>New extractor for input <em>{this.state.input.title}</em></span>}>
          <span>
            Extractors are applied on every message that is received by an input. Use them to extract and transform{' '}
            any text data into fields that allow you easy filtering and analysis later on.
          </span>

          <span>
            Find more information about extractors in the
            {' '}<DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
          </span>
        </PageHeader>
        <EditExtractor action="create"
                       extractor={this.state.extractor}
                       inputId={this.state.input.input_id}
                       exampleMessage={this.state.exampleMessage.fields[this.state.field]}
                       onSave={this._extractorSaved}/>
      </div>
    );
  },
});

export default CreateExtractorsPage;
