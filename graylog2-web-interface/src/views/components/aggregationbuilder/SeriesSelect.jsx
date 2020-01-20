// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { trim } from 'lodash';

import Select from 'views/components/Select';
import Series from 'views/logic/aggregationbuilder/Series';

import { parameterOptionsForType } from 'views/components/aggregationbuilder/SeriesParameterOptions';
import ConfigurableElement from './ConfigurableElement';
import SeriesConfiguration from './SeriesConfiguration';
import SeriesFunctionsSuggester from './SeriesFunctionsSuggester';
import CustomPropTypes from '../CustomPropTypes';

type Option = {|
  label: string,
  value: string,
  parameter?: string | number,
|};

type IncompleteOption = {| incomplete: true, parameterNeeded: false, value: string, label: string, value: string | number, parameter?: string | number |};
type ParameterNeededOption = {| incomplete: true, parameterNeeded: true, value: string, value: string |};
type BackToFunctions = {| label: string, backToFunctions: true |};

const parseSeries = (series: Array<Option>) => (series ? series.map(s => s.value) : []);

const newSeriesConfigChange = (values, series, newSeries, onChange) => {
  const newValues = values.map(s => (s === series ? newSeries : s));
  return onChange(newValues);
};

const _wrapOption = series => ({ label: series.effectiveName, value: series });

type Props = {
  onChange: (Array<Series>) => boolean,
  series: Array<Series>,
  suggester: ((string) => Array<Option>) & { defaults: Array<Option | IncompleteOption | ParameterNeededOption | BackToFunctions>, for: (string | number, ?(string | number)) => Array<Option> },
};

type State = {
  options: Array<Option | IncompleteOption | ParameterNeededOption | BackToFunctions>,
};

class SeriesSelect extends React.Component<Props, State> {
  static defaultProps = {
    suggester: new SeriesFunctionsSuggester(),
  };

  constructor(props: Props) {
    super(props);
    const { suggester } = props;
    this.state = {
      options: suggester.defaults,
    };
  }

  _onChange = (newSeries: Array<Option | IncompleteOption | ParameterNeededOption | BackToFunctions>) => {
    const { onChange, suggester } = this.props;
    const last = newSeries[newSeries.length - 1];

    if (last && last.parameterNeeded) {
      const options = parameterOptionsForType(last.value)
        .map(value => ({ label: value.toString(), value: last.value, parameterNeeded: false, incomplete: true, parameter: value }));

      this.setState({ options });
      return false;
    }

    if (last && last.incomplete) {
      const options = [].concat(
        [{ label: 'Back to function list', backToFunctions: true }],
        suggester.for(last.value, last.parameter),
      );
      this.setState({ options });
      return false;
    }

    this._resetToFunctions();

    if (last && last.backToFunctions) {
      return false;
    }

    // $FlowFixMe: Only `Option` present now.
    onChange(parseSeries(newSeries));
    return true;
  };

  _resetToFunctions = () => {
    const { suggester } = this.props;
    this.setState({ options: suggester.defaults });
  };

  _onClose = () => {
    this._resetToFunctions();
  };

  render() {
    const { onChange, series } = this.props;
    const { options } = this.state;
    const valueComponent = ({ children, innerProps, ...rest }) => {
      const element = rest.data.value;
      const { className } = innerProps;
      const usedNames = series.filter(s => s !== element)
        .map(s => (s && s.config && s.config.name ? s.config.name : null))
        .map(name => trim(name))
        .filter(name => name !== null && name !== undefined && name !== '');
      return (
        <span className={className}>
          <ConfigurableElement {...rest}
                               configuration={({ onClose }) => <SeriesConfiguration series={element} usedNames={usedNames} onClose={onClose} />}
                               onChange={newElement => newSeriesConfigChange(series, element, newElement, onChange)}
                               title="Series Configuration">
            {children}
          </ConfigurableElement>
        </span>
      );
    };
    const _components = {
      MultiValueLabel: valueComponent,
    };
    return (
      <Select placeholder="None: click to add series"
              onChange={this._onChange}
              options={options}
              value={series.map(_wrapOption)}
              components={_components}
              onClose={this._onClose}
              closeMenuOnSelect={false}
              menuShouldScrollIntoView
              tabSelectsValue
              escapeClearsValue
              isMulti />
    );
  }
}

SeriesSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  series: PropTypes.arrayOf(CustomPropTypes.instanceOf(Series)).isRequired,
  suggester: PropTypes.any,
};

export default SeriesSelect;
