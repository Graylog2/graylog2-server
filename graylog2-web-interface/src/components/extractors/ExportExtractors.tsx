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
import { useQuery } from '@tanstack/react-query';

import { ClipboardButton, Spinner } from 'components/common';
import { Row, Col, Input } from 'components/bootstrap';
import Version from 'util/Version';
import * as API from '@graylog/server-api';

const useExtractors = (inputId: string) => useQuery(['extractors', inputId], () => API.Extractors.list(inputId));

type Props = {
  id: string,
}

const skippedAttributes = ['id', 'metrics', 'creator_user_id', 'exceptions', 'converter_exceptions'];

const ExportExtractors = ({ id }: Props) => {
  const { data: extractors, isLoading } = useExtractors(id);

  if (isLoading) {
    return <Spinner />;
  }

  const extractorsExportObject = {
    // Create Graylog 1.x compatible export format.
    // TODO: This should be done on the server.
    extractors: extractors.extractors.map((extractor) => Object.fromEntries(Object.entries(extractor)
      .filter(([key]) => !skippedAttributes.includes(key))
      .map(([key, value]) => (key === 'type' ? ['extractor_type', value] : [key, value])))),
    version: Version.getFullVersion(),
  };

  const formattedJSON = JSON.stringify(extractorsExportObject, null, 2);

  return (
    <Row className="content">
      <Col md={12}>
        <Row>
          <Col md={8}>
            <h2>Extractors JSON</h2>
          </Col>
          <Col md={4}>
            <ClipboardButton title="Copy extractors" className="pull-right" text={formattedJSON} />
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <Input type="textarea" id="extractor-export-textarea" rows={30} value={formattedJSON} />
          </Col>
        </Row>
      </Col>
    </Row>
  );
};

export default ExportExtractors;
