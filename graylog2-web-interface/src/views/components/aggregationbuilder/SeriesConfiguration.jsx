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
import React from 'react';
import PropTypes from 'prop-types';
import { trim } from 'lodash';

import { Button, ControlLabel, FormControl, FormGroup, HelpBlock } from 'components/graylog';
import Series from 'views/logic/aggregationbuilder/Series';

export default class SeriesConfiguration extends React.Component {
  static propTypes = {
    series: PropTypes.instanceOf(Series).isRequired,
    onClose: PropTypes.func.isRequired,
    usedNames: PropTypes.arrayOf(PropTypes.string),
  };

  static defaultProps = {
    usedNames: [],
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      series: props.series,
      name: props.series.effectiveName,
    };
  }

  _onSubmit = () => {
    const { name, series, series: { config, function: functionName, effectiveName } } = this.state;
    const { onClose } = this.props;
    const newName = name || functionName;

    if (newName && newName !== effectiveName) {
      const newConfig = config.toBuilder().name(newName).build();
      const newSeries = series.toBuilder().config(newConfig).build();

      onClose(newSeries);
    } else {
      onClose(series);
    }
  };

  _changeName = (e) => this.setState({ name: e.target.value });

  render() {
    const { name } = this.state;
    const { usedNames = [] } = this.props;
    const isValid = !usedNames.includes(trim(name));
    const validationHint = isValid ? null : <> <strong>Name must be unique.</strong></>;

    return (
      <span>
        <FormGroup validationState={isValid ? null : 'error'}>
          <ControlLabel>Name</ControlLabel>
          <FormControl type="text" value={name} onChange={this._changeName} />
          <HelpBlock>The name of the series as it appears in the chart.{validationHint}</HelpBlock>
        </FormGroup>
        <div className="pull-right" style={{ marginBottom: '10px' }}>
          <Button bsStyle="success" disabled={!isValid} onClick={this._onSubmit}>Done</Button>
        </div>
      </span>
    );
  }
}
