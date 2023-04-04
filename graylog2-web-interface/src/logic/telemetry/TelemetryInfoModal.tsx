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
import {ConfirmDialog, Icon} from 'components/common';
import {Alert} from 'components/bootstrap';

type Props = {
  show: boolean,
  onConfirm: () => void,
}
const TelemetryInfoModal = ({show, onConfirm}: Props) => {

  return (
    <ConfirmDialog show={show} onConfirm={onConfirm} hideCancelButton={false} title="Help us improve Graylog"
                   btnConfirmText="Ok">
      <Alert bsStyle="info">
        <Icon name="info-circle" /> We would like to collect anonymously usage data to help us prioritize improvements
        and make Graylog better in the future.
        <br />
        We <b>do not</b> collect any personal data, any sensitive information or content such as logs that are in your
        instances.
        <br />
        You can turn data collection off or on any time in the <b>User profile settings</b>
      </Alert>
    </ConfirmDialog>
  );
};

TelemetryInfoModal.propTypes = {
  show: PropTypes.bool,
  onConfirm: PropTypes.func,
};

export default TelemetryInfoModal;
