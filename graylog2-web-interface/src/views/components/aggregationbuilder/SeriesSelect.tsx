/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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

type Option = {
  label: string,
  value: Series,
  parameter?: string | number,
};

type IncompleteOption = { incomplete: true, parameterNeeded: false, label: string, value: string | number, parameter?: string | number };
type ParameterNeededOption = { incomplete: true, parameterNeeded: true, value: string };
type BackToFunctions = { label: string, backToFunctions: true };

type IsOption = Option | IncompleteOption | ParameterNeededOption | BackToFunctions;

const parseSeries = (series: Array<Option>) => (series ? series.map((s) => s.value) : []);

const newSeriesConfigChange = (values, series, newSeries, onChange) => {
  const newValues = values.map((s) => (s === series ? newSeries : s));

  return onChange(newValues);
};

const _wrapOption = (series) => ({ label: series.effectiveName, value: series });

type Props = {
  onChange: (newSeries: Array<Series>) => boolean,
  series: Array<Series>,
  suggester: ((string) => Array<Option>) & {
    defaults: Array<Option | IncompleteOption | ParameterNeededOption | BackToFunctions>,
    for: (func: string | number, parameter: string | number | undefined | null) => Array<Option>,
  },
};

type State = {
  options: Array<Option | IncompleteOption | ParameterNeededOption | BackToFunctions>,
};

const isParameterNeeded = (option: IsOption): option is ParameterNeededOption => option && 'parameterNeeded' in option && option.parameterNeeded === true;

const isIncomplete = (option: IsOption): option is IncompleteOption => option && 'incomplete' in option && !option.parameterNeeded;

const isBackToFunctions = (option: IsOption): option is BackToFunctions => option && 'backToFunctions' in option && option.backToFunctions === true;

class SeriesSelect extends React.Component<Props, State> {
  static defaultProps = {
    suggester: new SeriesFunctionsSuggester(),
  };

  static propTypes = {
    onChange: PropTypes.func.isRequired,
    series: PropTypes.arrayOf(CustomPropTypes.instanceOf(Series)).isRequired,
    suggester: PropTypes.any,
  };

  constructor(props: Props) {
    super(props);
    const { suggester } = props;

    this.state = {
      options: suggester.defaults || [],
    };
  }

  _onChange = (newSeries: Array<Option | IncompleteOption | ParameterNeededOption | BackToFunctions>) => {
    const { onChange, suggester } = this.props;
    const last = newSeries[newSeries.length - 1];

    if (isParameterNeeded(last)) {
      const options = parameterOptionsForType(last.value)
        .map((value) => ({ label: value.toString(), value: last.value, parameterNeeded: false, incomplete: true, parameter: value }));

      this.setState({ options } as State);

      return false;
    }

    if (isIncomplete(last)) {
      const options = [].concat(
        [{ label: 'Back to function list', backToFunctions: true }],
        suggester.for(last.value, last.parameter),
      );

      this.setState({ options });

      return false;
    }

    this._resetToFunctions();

    if (isBackToFunctions(last)) {
      return false;
    }

    onChange(parseSeries(newSeries as Option[]));

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
      const usedNames = series.filter((s) => s !== element)
        .map((s) => (s && s.config && s.config.name ? s.config.name : null))
        .map((name) => trim(name))
        .filter((name) => name !== null && name !== undefined && name !== '');

      return (
        <span className={className}>
          <ConfigurableElement {...rest}
                               configuration={({ onClose }) => <SeriesConfiguration series={element} usedNames={usedNames} onClose={onClose} />}
                               onChange={(newElement) => newSeriesConfigChange(series, element, newElement, onChange)}
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
              closeMenuOnSelect={false}
              onMenuClose={this._onClose}
              menuShouldScrollIntoView
              escapeClearsValue
              isMulti />
    );
  }
}

export default SeriesSelect;
