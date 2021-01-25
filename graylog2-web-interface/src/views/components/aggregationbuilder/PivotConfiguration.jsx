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

import { Button } from 'components/graylog';
import FieldType from 'views/logic/fieldtypes/FieldType';

import TimeHistogramPivot from './pivottypes/TimeHistogramPivot';
import TermsPivotConfiguration from './pivottypes/TermsPivotConfiguration';

import CustomPropTypes from '../CustomPropTypes';

const _configurationComponentByType = (type, value, onChange) => {
  switch (type.type) {
    case 'date': return <TimeHistogramPivot onChange={onChange} value={value} />;
    default: return <TermsPivotConfiguration onChange={onChange} value={value} />;
  }
};

export default class PivotConfiguration extends React.Component {
  static propTypes = {
    type: CustomPropTypes.instanceOf(FieldType).isRequired,
    config: PropTypes.object.isRequired,
    onClose: PropTypes.func.isRequired,
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      config: props.config,
    };
  }

  _onSubmit = (e) => {
    e.preventDefault();
    e.stopPropagation();
    const { onClose } = this.props;

    onClose(this.state);
  };

  _onChange = (config) => this.setState({ config });

  render() {
    const { type } = this.props;
    const { config } = this.state;
    const typeSpecificConfiguration = _configurationComponentByType(type, config, this._onChange);

    return (
      <form onSubmit={this._onSubmit}>
        {typeSpecificConfiguration}
        <div className="pull-right" style={{ marginBottom: '10px' }}>
          <Button type="submit" bsStyle="success">Done</Button>
        </div>
      </form>
    );
  }
}
