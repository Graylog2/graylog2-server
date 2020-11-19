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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import moment from 'moment';

import { Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

const SystemInformation = createReactClass({
  displayName: 'SystemInformation',

  propTypes: {
    node: PropTypes.object.isRequired,
    systemInformation: PropTypes.object.isRequired,
    jvmInformation: PropTypes.object,
  },

  getInitialState() {
    return { time: moment() };
  },

  componentDidMount() {
    this.interval = setInterval(() => this.setState(this.getInitialState()), 1000);
  },

  componentWillUnmount() {
    clearTimeout(this.interval);
  },

  render() {
    const { systemInformation } = this.props;
    let jvmInformation;

    if (this.props.jvmInformation) {
      jvmInformation = <span>PID {this.props.jvmInformation.pid}, {this.props.jvmInformation.info}</span>;
    } else {
      jvmInformation = <span>JMV information for this node is unavailable.</span>;
    }

    return (
      <dl className="system-system">
        <dt>Hostname:</dt>
        <dd>{systemInformation.hostname}</dd>
        <dt>Node ID:</dt>
        <dd>{this.props.node.node_id}</dd>
        <dt>Version:</dt>
        <dd>{systemInformation.version}, codename <em>{systemInformation.codename}</em></dd>
        <dt>JVM:</dt>
        <dd>{jvmInformation}</dd>
        <dt>Time:</dt>
        <dd><Timestamp dateTime={this.state.time} format={DateTime.Formats.DATETIME_TZ} tz={systemInformation.timezone} /></dd>
      </dl>
    );
  },
});

export default SystemInformation;
