import React from 'react';
import PropTypes from 'prop-types';

import Select from 'react-select';
import ConfigurableElement from './ConfigurableElement';
import SeriesConfiguration from './SeriesConfiguration';
import SeriesFunctionsSuggester from './SeriesFunctionsSuggester';
import Series from '../../logic/aggregationbuilder/Series';

const parseSeries = series => (series ? series.map(s => s.value) : []);

const newSeriesConfigChange = (values, series, newSeries, onChange) => {
  const newValues = values.map(s => (s === series ? newSeries : s));
  return onChange(newValues);
};

const _wrapOption = series => ({ label: series.effectiveName, value: series });

class SeriesSelect extends React.Component {
  constructor(props, context) {
    super(props, context);
    const { suggester } = props;
    this.state = {
      options: suggester.defaults,
    };
  }
  _onChange = (newSeries) => {
    const last = newSeries[newSeries.length - 1];
    if (!last) {
      return false;
    }

    const { incomplete, backToFunctions, value } = last;

    if (incomplete) {
      const options = [].concat(
        [{ label: 'Back to function list', backToFunctions: true }],
        this.props.suggester.for(value),
      );
      this.setState({ options });
      return false;
    }

    this._resetToFunctions();

    if (backToFunctions) {
      return false;
    }

    this.props.onChange(parseSeries(newSeries));
    return true;
  };

  _resetToFunctions = () => {
    this.setState({ options: this.props.suggester.defaults });
  };

  _onClose = () => {
    this._resetToFunctions();
  };

  render() {
    const { onChange, series } = this.props;
    const valueComponent = ({ children, ...rest }) => {
      const element = rest.value.value;
      return (
        <ConfigurableElement {...rest}
                             configuration={({ onClose }) => <SeriesConfiguration series={element} onClose={onClose} />}
                             onChange={newElement => newSeriesConfigChange(series, element, newElement, onChange)}
                             title="Series Configuration">
          {children}
        </ConfigurableElement>
      );
    };
    return (<Select placeholder="None: click to add series"
                    onChange={this._onChange}
                    options={this.state.options}
                    value={series.map(_wrapOption)}
                    valueComponent={valueComponent}
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
  onChange: PropTypes.func.isRequired,
  series: PropTypes.arrayOf(PropTypes.instanceOf(Series)).isRequired,
  suggester: PropTypes.any,
};

SeriesSelect.defaultProps = {
  suggester: new SeriesFunctionsSuggester(),
};

export default SeriesSelect;
