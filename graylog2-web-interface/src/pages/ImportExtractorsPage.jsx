import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ImportExtractors from 'components/extractors/ImportExtractors';

import ActionsProvider from 'injection/ActionsProvider';

import StoreProvider from 'injection/StoreProvider';

const InputsActions = ActionsProvider.getActions('Inputs');
const InputsStore = StoreProvider.getStore('Inputs');

const ImportExtractorsPage = createReactClass({
  displayName: 'ImportExtractorsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(InputsStore)],

  componentDidMount() {
    const { params } = this.props;
    InputsActions.get.triggerPromise(params.inputId).then((input) => this.setState({ input: input }));
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
      <DocumentTitle title={`Import extractors to ${input.title}`}>
        <div>
          <PageHeader title={<span>Import extractors to <em>{input.title}</em></span>}>
            <span>
              Exported extractors can be imported to an input. All you need is the JSON export of extractors from any
              other Graylog setup or from{' '}
              <a href="https://marketplace.graylog.org/" rel="noopener noreferrer" target="_blank">
                the Graylog Marketplace
              </a>.
            </span>
          </PageHeader>
          <ImportExtractors input={input} />
        </div>
      </DocumentTitle>
    );
  },
});

export default ImportExtractorsPage;
