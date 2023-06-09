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

import { Button, Col, Row } from 'components/bootstrap';
import { IconButton } from 'components/common';

import Errors from './Errors';
import type { RuleBlock } from './types';
import { ruleBlockPropType } from './types';
import { paramValueExists, paramValueIsVariable } from './helpers';

type Props = {
  block: RuleBlock,
  negatable?: boolean,
  onDelete: () => void,
  onEdit: () => void,
  onNegate: () => void,
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

const NegationButton = styled(Button).attrs(({ negate }: { negate: boolean }) => ({
  negate,
}))(({ negate, theme }) => css`
  opacity: ${negate ? '1' : '0.3'};
  margin-right: ${theme.spacings.sm};
`);

const RuleBlockDisplay = ({ block, negatable, onEdit, onDelete, onNegate } : Props) => {
  const paramNames = Object.keys(block.params);

  const anyParamsSet = () : boolean => (
    paramNames.some((paramName) => paramValueExists(block.params[paramName]))
  );

  const formatParamValue = (value : string | number | boolean) => {
    if (paramValueIsVariable(value)) {
      return 'Output of the previous step';
    }

    return (value.toString());
  };

  return (
    <Row>
      <Col xs={9} md={10}>
        <BlockInfo>
          <Col md={12}>
            <h3>
              {negatable
              && <NegationButton bsStyle="primary" negate={block?.negate} onClick={(e) => { e.target.blur(); onNegate(); }}>Not</NegationButton>}
              {block?.step_title}
            </h3>
          </Col>
        </BlockInfo>
        {anyParamsSet
        && (
        <Row>
          <ParamsCol sm={12} md={6}>
            {paramNames.map((paramName) => {
              const paramValue = block.params[paramName];

              if (paramValueExists(paramValue)) {
                return (
                  <Col key={paramName}>
                    <Param><strong>{paramName}:</strong> {formatParamValue(paramValue)}</Param>
                  </Col>
                );
              }

              return null;
            })}
          </ParamsCol>
        </Row>
        )}
        <Errors objectWithErrors={block} />
      </Col>
      <Col xs={3} md={2} className="text-right">
        <IconButton name="edit" onClick={onEdit} title="Edit" />
        <IconButton name="trash-alt" onClick={onDelete} title="Delete" />
      </Col>
    </Row>
  );
};

RuleBlockDisplay.propTypes = {
  block: ruleBlockPropType,
  onDelete: PropTypes.func.isRequired,
  onEdit: PropTypes.func.isRequired,
  negatable: PropTypes.bool,
  onNegate: PropTypes.func.isRequired,
};

RuleBlockDisplay.defaultProps = {
  block: undefined,
  negatable: false,
};

export default RuleBlockDisplay;
