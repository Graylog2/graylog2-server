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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Col, Row } from 'components/bootstrap';
import { IconButton } from 'components/common';

import type { RuleBlock, BlockDict } from './types';
import { ruleBlockPropType, blockDictPropType } from './types';

type Props = {
  block: RuleBlock,
  blockDict: BlockDict,
  onDelete: () => void,
  onEdit: () => void,
}

const BlockInfo = styled(Row)(({ theme }) => css`
  margin-bottom: ${theme.spacings.md};
`);

const ParamsCol = styled(Col)(({ theme }) => css`
  display: flex;
  flex-wrap: wrap;
  gap: ${theme.spacings.sm};
`);

const Param = styled.p`
  margin-bottom: 0;
`;

const RuleBlockDisplay = ({ block, blockDict, onEdit, onDelete } : Props) => {
  const paramNames = Object.keys(block.params);

  const paramValueExists = (paramValue: string | number | boolean) : boolean => (
    paramValue && paramValue !== '' && paramValue !== null
  );

  const anyParamsSet = () : boolean => (
    paramNames.some((paramName) => paramValueExists(block.params[paramName]))
  );

  const formatParamValue = (value : string | number | boolean) => {
    if (typeof value === 'string' && value.startsWith('$')) {
      return 'Output of the previous step';
    }

    return (value);
  };

  return (
    <Row>
      <Col xs={9} md={10}>
        <BlockInfo>
          <Col md={12}>
            <h3>{blockDict?.title || blockDict?.rule_builder_title || blockDict?.name}</h3>
          </Col>
        </BlockInfo>
        {anyParamsSet
        && (
        <Row>
          <ParamsCol sm={12} md={6}>
            {paramNames.map((paramName, key) => {
              const paramValue = formatParamValue(block.params[paramName]);

              if (paramValueExists(paramValue)) {
                return (
                // eslint-disable-next-line react/no-array-index-key
                  <Col key={key}>
                    <Param><strong>{paramName}:</strong> {paramValue}</Param>
                  </Col>
                );
              }

              return null;
            })}
          </ParamsCol>
        </Row>
        )}
      </Col>
      <Col xs={3} md={2} className="text-right">
        <IconButton name="edit" onClick={onEdit} title="Edit" />
        <IconButton name="trash" onClick={onDelete} title="Delete" />
      </Col>
    </Row>
  );
};

RuleBlockDisplay.propTypes = {
  block: ruleBlockPropType,
  blockDict: blockDictPropType,
  onDelete: PropTypes.func.isRequired,
  onEdit: PropTypes.func.isRequired,
};

RuleBlockDisplay.defaultProps = {
  block: undefined,
  blockDict: undefined,
};

export default RuleBlockDisplay;
