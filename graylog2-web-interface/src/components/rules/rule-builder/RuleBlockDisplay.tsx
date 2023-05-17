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

import { Button, Col, Row } from 'components/bootstrap';

import type { BlockDict, RuleBlock } from './types';

type Props = {
  block: RuleBlock,
  blockDict: BlockDict,
  onDelete: () => void,
  onEdit: () => void,
}

const RuleBlockDisplay = ({ block, blockDict, onEdit, onDelete }:Props) => (
  <Row>
    <Col md={12}>
      <Row>
        <Col md={12}>
          <h3>{blockDict?.rule_builder_title || blockDict?.name}</h3>
        </Col>
      </Row>
      {Object.keys(block.params).map((paramName) => {
        const paramValue = block.params[paramName];
        const paramValueExists = paramValue && paramValue !== '' && paramValue !== null;

        if (paramValueExists) {
          return (
            <Row>
              <Col md={12}>
                <p><strong>{paramName}:</strong> {paramValue}</p>
              </Col>
            </Row>
          );
        }

        return null;
      })}
      <Row>
        <Col md={12}>
          <Button onClick={onEdit}>Edit</Button>
          <Button onClick={onDelete}>Delete</Button>
        </Col>
      </Row>
    </Col>
  </Row>
);

export default RuleBlockDisplay;
