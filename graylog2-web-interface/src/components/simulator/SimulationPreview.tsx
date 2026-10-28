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
import { Trans, useTranslation } from 'react-i18next';

import { Alert } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';

type SimulationPreviewProps = {
  simulationResults: any;
  streams: any;
};

const SimulationPreview = ({ simulationResults, streams }: SimulationPreviewProps) => {
  const { t } = useTranslation('pipelines');
  const { messages } = simulationResults;

  if (messages.length === 0) {
    return (
      <Alert bsStyle="info" title={t('simulation_preview.dropped_title')}>
        <p>
          <Trans t={t} i18nKey="simulation_preview.dropped" components={{ notStored: <strong /> }} />
        </p>
      </Alert>
    );
  }

  const formattedMessages = messages.map((message) => (
    <MessageShow key={message.id} message={message} streams={streams} />
  ));

  return <div className="message-preview-wrapper">{formattedMessages}</div>;
};

export default SimulationPreview;
