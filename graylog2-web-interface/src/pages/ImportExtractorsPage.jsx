import React, { PropTypes } from 'react';
import Reflux from 'reflux';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ImportExtractors from 'components/extractors/ImportExtractors';

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');

import StoreProvider from 'injection/StoreProvider';
const InputsStore = StoreProvider.getStore('Inputs');

const ImportExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(InputsStore)],
  getInitialState() {
    return {
      input: undefined,
    };
  },
  componentDidMount() {
    InputsActions.get.triggerPromise(this.props.params.inputId).then(input => this.setState({ input: input }));
  },
  _isLoading() {
    return !this.state.input;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title={`Import extractors to ${this.state.input.title}`}>
        <div>
          <PageHeader title={<span>Import extractors to <em>{this.state.input.title}</em></span>}>
            <span>
              Exported extractors can be imported to an input. All you need is the JSON export of extractors from any
              other Graylog setup or from <a href="https://marketplace.graylog.org/" target="_blank">the Graylog
              Marketplace</a>.
            </span>
          </PageHeader>
          <ImportExtractors input={this.state.input} />
        </div>
      </DocumentTitle>
    );
  },
});

export default ImportExtractorsPage;
