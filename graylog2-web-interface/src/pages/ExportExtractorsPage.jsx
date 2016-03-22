import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';
import ExportExtractors from 'components/extractors/ExportExtractors';

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');

import StoreProvider from 'injection/StoreProvider';
const InputsStore = StoreProvider.getStore('Inputs');

const ExportExtractorsPage = React.createClass({
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
    InputsActions.get.triggerPromise(this.props.params.inputId);
  },
  _isLoading() {
    return !this.state.input;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <div>
        <PageHeader title={<span>Export extractors of <em>{this.state.input.title}</em></span>}>
          <span>
            The extractors of an input can be exported to JSON for importing into other setups
            or sharing in <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
          </span>
        </PageHeader>
        <ExportExtractors input={this.state.input}/>
      </div>
    );
  },
});

export default ExportExtractorsPage;
