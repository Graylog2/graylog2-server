import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import Spinner from 'components/common/Spinner';
import PageHeader from 'components/common/PageHeader';
import DocumentationLink from 'components/support/DocumentationLink';
import EditExtractor from 'components/extractors/EditExtractor';

import DocsHelper from 'util/DocsHelper';

import InputsStore from 'stores/inputs/InputsStore';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';
import ExtractorsStore from 'stores/extractors/ExtractorsStore';

const EditExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(ExtractorsStore), Reflux.ListenerMethods],
  getInitialState() {
    return {
      extractor: undefined,
      input: undefined,
    };
  },
  componentDidMount() {
    InputsStore.get(this.props.params.inputId).then(input => this.setState({input: input}));
    ExtractorsActions.get.triggerPromise(this.props.params.inputId, this.props.params.extractorId);
  },
  _isLoading() {
    return !(this.state.input && this.state.extractor);
  },
  render() {
    // TODO:
    // - Load recent message from input
    // - Redirect when extractor or input were deleted

    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <div>
        <PageHeader title={<span>Edit extractor <em>{this.state.extractor.title}</em> for input <em>{this.state.input.title}</em></span>}>
          <span>
            Extractors are applied on every message that is received by an input. Use them to extract and transform{' '}
            any text data into fields that allow you easy filtering and analysis later on.
          </span>

          <span>
            Find more information about extractors in the
            {' '}<DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
          </span>
        </PageHeader>
        <EditExtractor extractor={this.state.extractor} inputId={this.state.input.input_id} />
      </div>
    );
  },
});

export default EditExtractorsPage;
