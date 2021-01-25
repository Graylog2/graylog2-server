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

import { Alert } from 'components/graylog';
import MessageShow from 'components/search/MessageShow';

const SimulationPreview = ({ simulationResults, streams }) => {
  const { messages } = simulationResults;

  if (messages.length === 0) {
    return (
      <Alert bsStyle="info">
        <p><strong>Message would be dropped</strong></p>
        <p>
          The pipeline processor would drop such a message. That means that the message
          <strong>would not be stored</strong>, and would not be available for searches, alerts, outputs, or dashboards.
        </p>
      </Alert>
    );
  }

  const formattedMessages = messages.map((message) => {
    return (
      <MessageShow key={message.id}
                   message={message}
                   streams={streams} />
    );
  });

  return <div className="message-preview-wrapper">{formattedMessages}</div>;
};

SimulationPreview.propTypes = {
  simulationResults: PropTypes.object.isRequired,
  streams: PropTypes.object.isRequired,
};

export default SimulationPreview;
