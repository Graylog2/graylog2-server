// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import Select from 'enterprise/components/Select';
import Series from 'enterprise/logic/aggregationbuilder/Series';

import ConfigurableElement from './ConfigurableElement';
import SeriesConfiguration from './SeriesConfiguration';
import SeriesFunctionsSuggester from './SeriesFunctionsSuggester';

type Option = {|
  label: string,
  value: string,
|};

type IncompleteOption = {| incomplete: true, value: string |};
type BackToFunctions = {| label: string, backToFunctions: true |};

const parseSeries = (series: Array<Option>) => (series ? series.map(s => s.value) : []);

const newSeriesConfigChange = (values, series, newSeries, onChange) => {
  const newValues = values.map(s => (s === series ? newSeries : s));
  return onChange(newValues);
};

const _wrapOption = series => ({ label: series.effectiveName, value: series });

type Props = {
  onChange: (Array<*>) => boolean,
  series: Array<Series>,
  suggester: (string) => Array<Option>,
};

type State = {
  options: Array<Option | IncompleteOption | BackToFunctions>,
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
  _onChange = (newSeries: Array<Option | IncompleteOption | BackToFunctions>) => {
    const last = newSeries[newSeries.length - 1];
    if (!last) {
      return false;
    }

    if (last.incomplete) {
      const options = [].concat(
        [{ label: 'Back to function list', backToFunctions: true }],
        this.props.suggester.for(last.value),
      );
      this.setState({ options });
      return false;
    }

    this._resetToFunctions();

    if (last.backToFunctions) {
      return false;
    }

    // $FlowFixMe: Only `Option` present now.
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
    const valueComponent = ({ children, innerProps, ...rest }) => {
      const element = rest.data.value;
      const { className } = innerProps;
      return (
        <span className={className}>
          <ConfigurableElement {...rest}
                               configuration={({ onClose }) => <SeriesConfiguration series={element} onClose={onClose} />}
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
    return (<Select placeholder="None: click to add series"
                    onChange={this._onChange}
                    options={this.state.options}
                    value={series.map(_wrapOption)}
                    components={_components}
                    onClose={this._onClose}
                    closeMenuOnSelect={false}
                    onBlurResetsInput
                    onCloseResetsInput
                    menuShouldScrollIntoView
                    tabSelectsValue
                    escapeClearsValue
                    isMulti />);
  }
}

SeriesSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  series: PropTypes.arrayOf(PropTypes.instanceOf(Series)).isRequired,
  suggester: PropTypes.any,
};

export default SeriesSelect;
