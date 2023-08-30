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
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Button, Col, Label, Row } from 'components/bootstrap';
import { IconButton } from 'components/common';

import Errors from './Errors';
import type { RuleBlock } from './types';
import { ruleBlockPropType, RuleBuilderTypes } from './types';

type Props = {
  block: RuleBlock,
  negatable?: boolean,
  onDelete: () => void,
  onEdit: () => void,
  onNegate: () => void,
  returnType?: RuleBuilderTypes,
}

const Highlighted = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.info};
  font-weight: bold;
`);

const TypeLabel = styled(Label)(({ theme }) => css`
  margin-left: ${theme.spacings.xs};
`);

const StyledRow = styled(Row)<{ $hovered: boolean }>(({ theme, $hovered }) => css`
  cursor: pointer;
  display: flex;
  align-items: center;
  margin: 0px;
  height: ${theme.spacings.xl};
  background-color: ${$hovered ? '#f5f5f5' : 'transparent'};
`);

const NegationButton = styled(Button)<{ $negate: boolean }>(({ theme, $negate }) => css`
  opacity: ${$negate ? '1' : '0.3'};
  margin-right: ${theme.spacings.sm};
`);

const RuleBlockDisplay = ({ block, negatable, onEdit, onDelete, onNegate, returnType } : Props) => {
  const [showActions, setShowActions] = useState<boolean>(false);

  const readableReturnType = (type: RuleBuilderTypes): string | undefined => {
    switch (type) {
      case RuleBuilderTypes.Boolean:
        return 'boolean';
      case RuleBuilderTypes.Message:
        return 'message';
      case RuleBuilderTypes.Number:
        return 'number';
      case RuleBuilderTypes.Object:
        return 'object';
      case RuleBuilderTypes.String:
        return 'string';
      case RuleBuilderTypes.Void:
        return 'void';
      case RuleBuilderTypes.DateTime:
        return 'date_time';
      case RuleBuilderTypes.DateTimeZone:
        return 'date_time_zone';
      case RuleBuilderTypes.DateTimeFormatter:
        return 'date_time_formatter';
      default:
        return undefined;
    }
  };

  const returnTypeLabel = readableReturnType(returnType);

  const highlightedRuleTitle = (termToHighlight: string, title: string = '') => {
    const parts = title.split("'");

    const partsWithHighlight = parts.map((part) => {
      if (part === `$${termToHighlight}`) {
        return <Highlighted>{part}</Highlighted>;
      }

      return part;
    });

    return (
      partsWithHighlight.map((item, index) => (
        // eslint-disable-next-line react/no-array-index-key
        <React.Fragment key={index}>
          {item}
        </React.Fragment>
      )));
  };

  const highlightedOutput = undefined; // TODO: Set and update in context
  const setHighlightedOutput = (outputToHighlight: string) => { console.log('Setting highlighted output', outputToHighlight); }; // TODO: Replace this with function from context

  return (
    <StyledRow onMouseEnter={() => setShowActions(true)}
               onMouseLeave={() => setShowActions(false)}
               $hovered={showActions}>
      <Col xs={showActions ? 9 : 12} md={showActions ? 10 : 12}>
        <Row>
          <Col md={12}>
            <h5>
              {negatable
              && <NegationButton bsStyle="primary" bsSize="xs" $negate={block?.negate ? 1 : 0} onClick={(e) => { e.target.blur(); onNegate(); }}>Not</NegationButton>}
              {highlightedOutput ? (highlightedRuleTitle(highlightedOutput, block?.step_title)) : block?.step_title}
              {block?.outputvariable && (
              <>
                &nbsp;&nbsp;
                <Label bsStyle="primary" onHover={() => setHighlightedOutput(block.outputvariable)}>
                  {`$${block.outputvariable}`}
                </Label>
                {returnTypeLabel && (
                <TypeLabel bsStyle="default">
                  {returnTypeLabel}
                </TypeLabel>
                )}
              </>
              )}
            </h5>
          </Col>
        </Row>
        <Errors objectWithErrors={block} />
      </Col>
      {showActions && (
        <Col xs={3} md={2} className="text-right">
          <IconButton name="edit" onClick={onEdit} title="Edit" />
          <IconButton name="trash-alt" onClick={onDelete} title="Delete" />
        </Col>
      )}
    </StyledRow>
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
  returnType: undefined,
};

export default RuleBlockDisplay;
