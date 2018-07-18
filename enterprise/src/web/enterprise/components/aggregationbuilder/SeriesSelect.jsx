import React from 'react';
import PropTypes from 'prop-types';
import { flatten } from 'lodash';

import connect from 'stores/connect';
import Select from 'react-select';
import AggregationFunctionsStore from 'enterprise/stores/AggregationFunctionsStore';
import { FieldList } from './AggregationBuilderPropTypes';

const parseSeries = series => (series ? series.map(s => s.value) : []);

const combineFunctionsWithFields = (functions, fields) => flatten(fields.map(name => functions.map(f => `${f}(${name})`)));

const _wrapOption = opt => ({ label: opt, value: opt });

const _makeIncompleteFunction = fun => ({ label: `${fun}(...)`, value: fun, incomplete: true });

class SeriesSelect extends React.Component {
  constructor(props, context) {
    super(props, context);
    const { functions } = props;
    this.state = {
      options: Object.keys(functions).map(_makeIncompleteFunction),
    };
  }
  _onChange = (newSeries) => {
    const last = newSeries[newSeries.length - 1];
    const { fields } = this.props;

    if (last && last.incomplete) {
      const options = [].concat(
        [{ label: 'Back to function list', backToFunctions: true }],
        combineFunctionsWithFields([last.value], fields).map(_wrapOption),
      );
      this.setState({ options });
      return false;
    }

    this._resetToFunctions();

    if (last && last.backToFunctions) {
      return false;
    }

    this.props.onChange(parseSeries(newSeries));
    return true;
  };

  _resetToFunctions = () => {
    const { functions } = this.props;
    this.setState({ options: Object.keys(functions).map(_makeIncompleteFunction) });
  };

  _onClose = () => {
    this._resetToFunctions();
  };

  render() {
    const { series } = this.props;
    return (<Select placeholder="Click to add Series..."
                    onChange={this._onChange}
                    options={this.state.options}
                    value={series.map(_wrapOption)}
                    onClose={this._onClose}
                    ignoreCase
                    closeOnSelect={false}
                    openOnClick
                    openOnFocus
                    onBlurResetsInput
                    onCloseResetsInput
                    scrollMenuIntoView
                    tabSelectsValue
                    escapeClearsValue
                    multi />);
  }
}

SeriesSelect.propTypes = {
  fields: FieldList.isRequired,
  functions: PropTypes.objectOf(PropTypes.shape({
    type: PropTypes.string,
  })).isRequired,
  onChange: PropTypes.func.isRequired,
  series: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default connect(SeriesSelect, { functions: AggregationFunctionsStore });
