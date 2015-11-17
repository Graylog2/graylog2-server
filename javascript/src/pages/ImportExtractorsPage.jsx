import React, {PropTypes} from 'react';

import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';
import ImportExtractors from 'components/extractors/ImportExtractors';

import InputsStore from 'stores/inputs/InputsStore';

const ImportExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      input: undefined,
    };
  },
  componentDidMount() {
    InputsStore.get(this.props.params.inputId).then(input => this.setState({input: input}));
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
        <PageHeader title={<span>Import extractors to <em>{this.state.input.title}</em></span>}>
          <span>
            Exported extractors can be imported to an input. All you need is the JSON export of extractors from any
            other Graylog setup or from <a href="https://marketplace.graylog.org/" target="_blank">the Graylog
            Marketplace</a>.
          </span>
        </PageHeader>
        <ImportExtractors input={this.state.input}/>
      </div>
    );
  },
});

export default ImportExtractorsPage;
