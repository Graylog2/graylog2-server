import React from 'react';

import { Icon, ExternalLink } from 'components/common';
import { Alert } from 'components/bootstrap';

type Props = {
  showProfile?: boolean
}

const TelemetryInfoText = ({ showProfile }: Props) => {
  return (
    <Alert bsStyle="info">
      <Icon name="info-circle" /> We would like to collect anonymously usage data to help us prioritize improvements
      and make Graylog better in the future.
      <br />
      We <b>do not</b> collect any personal data, any sensitive information or content such as logs that are in your
      instances.
      <br />
      Learn more on our <ExternalLink href="https://www.graylog.com/">Privacy Policy</ExternalLink>.
      <br />
      You can turn data collection off or on any time
      {showProfile && <b> on User profile</b>}
      {!showProfile && ' here'}.
    </Alert>
  );
};

TelemetryInfoText.defaultProps = {
  showProfile: undefined,
};

export default TelemetryInfoText;
