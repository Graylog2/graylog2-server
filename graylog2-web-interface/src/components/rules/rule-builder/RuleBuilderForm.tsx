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

import { Input, Row, Col } from 'components/bootstrap';

import type { RuleBuilderRule } from './types';

import PipelinesUsingRule from '../PipelinesUsingRule';

type Props = {
  rule: RuleBuilderRule,
  onChange: (rule: RuleBuilderRule) => void,
};

const RuleBuilderForm = ({ rule, onChange }: Props) => {
  const handleTitleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange({ ...rule, title: event.target.value });
  };

  const handleDescriptionChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange({ ...rule, description: event.target.value });
  };

  return (
    <fieldset>
      <Row>
        <Col xs={6}>
          <Input type="text"
                 id="title"
                 label="Title"
                 value={rule.title}
                 onChange={handleTitleChange}
                 autoFocus
                 required
                 help="Rule title." />
        </Col>
        <Col xs={6}>
          <Input type="textarea"
                 id="description"
                 label="Description"
                 value={rule.description}
                 onChange={handleDescriptionChange}
                 rows={1}
                 help="Rule description (optional)." />
        </Col>
      </Row>

      <PipelinesUsingRule create={Boolean(rule)} />
    </fieldset>
  );
};

export default RuleBuilderForm;
