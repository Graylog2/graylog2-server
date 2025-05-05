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

import { Col, Row } from 'components/bootstrap';
import EntityCreateShareFormGroup from 'components/permissions/EntityCreateShareFormGroup';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import commonStyles from '../common/commonStyles.css';

type Props = {
  onChange: (name: string, value: EntitySharePayload) => void;
};

const ShareForm = ({ onChange }: Props) => {
  const handleEntityShareSet = (entityShare?: EntitySharePayload) => onChange('share_request', entityShare);

  return (
    <Row>
      <Col md={6} lg={6}>
        <h2 className={commonStyles.title}>Share <small>(optional)</small></h2>
        <EntityCreateShareFormGroup
          description='Search for a User or Team to add as collaborator on this event definition.'
          onSetEntityShare={handleEntityShareSet} entityType='event_definition' entityTitle=''/>
      </Col>
    </Row>
  );
};

export default ShareForm;
