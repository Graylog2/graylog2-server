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
