import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ExportExtractors from 'components/extractors/ExportExtractors';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';

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

export default ExportExtractorsPage;
