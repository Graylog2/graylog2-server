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
import styled from 'styled-components';

import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';

import type { BlockDict, RuleBlock } from './types';

type Props = {
  block: RuleBlock,
  blockDict: BlockDict,
  onDelete: () => void,
  onEdit: () => void,
}

const BlockInfo = styled(Row)`
  margin-bottom: 20px;
`;

const ActionsRow = styled(Row)`
  margin-top: 20px;
`;

const ParamsRow = styled(Row)`
  display: flex;
  flex-wrap: wrap;
  max-width: 50%;
`;

const ParamCol = styled(Col)`
  padding-right: 15px;
  padding-left: 15px;
`;

const RuleBlockDisplay = ({ block, blockDict, onEdit, onDelete }:Props) => {
  const paramNames = Object.keys(block.params);

  const paramValueExists = (paramValue: string | number | boolean) : boolean => (
    paramValue && paramValue !== '' && paramValue !== null
  );

  const anyParamsSet = () : boolean => (
    paramNames.some((paramName) => paramValueExists(block.params[paramName]))
  );

  return (
    <Row>
      <Col md={12}>
        <BlockInfo>
          <Col md={12}>
            <h3>{blockDict?.rule_builder_title || blockDict?.name}</h3>
          </Col>
        </BlockInfo>
        {anyParamsSet
        && (
        <ParamsRow>
          {paramNames.map((paramName, key) => {
            const paramValue = block.params[paramName];

            if (paramValueExists(paramValue)) {
              return (
              // eslint-disable-next-line react/no-array-index-key
                <ParamCol key={key} sm="auto" md="auto" lg="auto">
                  <p><strong>{paramName}:</strong> {paramValue}</p>
                </ParamCol>
              );
            }

            return null;
          })}
        </ParamsRow>
        )}

        <ActionsRow>
          <Col md={12}>
            <ButtonToolbar>
              <Button bsStyle="success" bsSize="small" onClick={onEdit}>Edit</Button>
              <Button bsSize="small" onClick={onDelete}>Delete</Button>
            </ButtonToolbar>
          </Col>
        </ActionsRow>
      </Col>
    </Row>
  );
};

export default RuleBlockDisplay;
