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

import { Row, Col } from 'components/graylog';
import { Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

import 'components/content-packs/ContentPackDetails.css';
import ContentPackInstallEntityList from './ContentPackInstallEntityList';

const ContentPackInstallView = (props) => {
  const { comment } = props.install;
  const createdAt = props.install.created_at;
  const createdBy = props.install.created_by;

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
            <dd><Timestamp dateTime={createdAt} format={DateTime.Formats.COMPLETE} tz="browser" /></dd>
          </dl>
        </Col>
      </Row>
      <Row>
        <Col smOffset={1} sm={10}>
          <ContentPackInstallEntityList entities={props.install.entities} />
        </Col>
      </Row>
    </div>
  );
};

ContentPackInstallView.propTypes = {
  install: PropTypes.object.isRequired,
};

export default ContentPackInstallView;
