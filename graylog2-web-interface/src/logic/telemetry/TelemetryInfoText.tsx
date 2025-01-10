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

import { ExternalLink } from 'components/common';
import { Alert } from 'components/bootstrap';

type Props = {
  showProfile?: boolean
}

const TelemetryInfoText = ({ showProfile }: Props) => (
  <Alert bsStyle="info">
    We would like to collect anonymous usage data to help us prioritize improvements
    and make Graylog better in the future.
    <br />
    We do not collect personal data, sensitive information, or content such as logs in your
    instances.
    <br />
    Learn more on our <ExternalLink href="https://www.graylog.org/privacy-policy/">Privacy Policy</ExternalLink>.
    <br />
    You can turn data collection off or on any time
    {showProfile && <b> in the user profile</b>}
    {!showProfile && ' here'}.
  </Alert>
);

export default TelemetryInfoText;
