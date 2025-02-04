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

import { Timestamp } from 'components/common';
import { Row, Col } from 'components/bootstrap';

import 'components/content-packs/ContentPackDetails.css';
import ContentPackInstallEntityList from './ContentPackInstallEntityList';

type ContentPackInstallViewProps = {
  install: {
    comment: string,
    created_at: string,
    created_by: string,
    entities: any[],
  };
};

const ContentPackInstallView = ({ install }: ContentPackInstallViewProps) => {
  const { comment, created_at: createdAt, created_by: createdBy, entities } = install;

  return (
    <div>
      <Row>
        <Col smOffset={1} sm={10}>
          <h3>General information</h3>
          <dl className="deflist">
            <dt>Comment:</dt>
            <dd>{comment}</dd>
            <dt>Installed by:</dt>
            <dd>{createdBy}&nbsp;</dd>
            <dt>Installed at:</dt>
            <dd><Timestamp dateTime={createdAt} /></dd>
          </dl>
        </Col>
      </Row>
      <Row>
        <Col smOffset={1} sm={10}>
          <ContentPackInstallEntityList entities={entities} />
        </Col>
      </Row>
    </div>
  );
};

export default ContentPackInstallView;
