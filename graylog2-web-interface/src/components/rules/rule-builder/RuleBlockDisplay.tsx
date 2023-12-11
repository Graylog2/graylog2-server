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

import { Button, Col, Label, Row, MenuItem } from 'components/bootstrap';
import { IconButton } from 'components/common';
import { MORE_ACTIONS_TITLE, MORE_ACTIONS_HOVER_TITLE } from 'components/common/EntityDataTable/Constants';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';

import type { BlockType, RuleBlock } from './types';
import { ruleBlockPropType, RuleBuilderTypes } from './types';
import { useRuleBuilder } from './RuleBuilderContext';

type Props = {
  block: RuleBlock,
  negatable?: boolean,
  onDelete: () => void,
  onEdit: () => void,
  onNegate: () => void,
  onDuplicate: () => void,
  onInsertAbove: () => void,
  onInsertBelow: () => void,
  returnType?: RuleBuilderTypes,
  type: BlockType,
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
  margin: 0;
  height: ${theme.spacings.xl};
  background-color: ${$hovered ? theme.colors.table.backgroundHover : 'transparent'};
  border-left: solid 1px ${theme.colors.gray[80]};
`);

type NegationButtonProps = React.ComponentProps<typeof Button> & { $negate: boolean };
const NegationButton: React.ComponentType<NegationButtonProps> = styled(Button)<{ $negate: boolean }>(({ theme, $negate }) => css`
  opacity: ${$negate ? '1' : '0.3'};
  margin-right: ${theme.spacings.sm};
`);

const BlockTitle = styled.h5`
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
`;

const ErrorMessage = styled.p(({ theme }) => css`
  color: ${theme.colors.variant.danger};
  font-size: 0.75rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  margin: 0;
`);

const ActionsContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: end;
`;

const EditIconButton = styled(IconButton)(({ theme }) => css`
  margin-right: ${theme.spacings.xs};
`);

const RuleBlockDisplay = ({ block, negatable, onEdit, onDelete, onNegate, onDuplicate, onInsertAbove, onInsertBelow, returnType, type } : Props) => {
  const [showActions, setShowActions] = useState<boolean>(false);
  const [highlightedOutput, setHighlightedOutput] = useRuleBuilder().useHighlightedOutput;

  const readableReturnType = (_type: RuleBuilderTypes): string | undefined => {
    switch (_type) {
      case RuleBuilderTypes.Number:
        return 'Number';
      default:
        return _type?.slice((_type?.lastIndexOf('.') || 0) + 1);
    }
  };

  const returnTypeLabel = readableReturnType(returnType);

  const highlightedRuleTitle = (termToHighlight: string, title: string = '') => {
    const parts = title.split(/('\$.*?')/);

    const partsWithHighlight = parts.map((part) => {
      if (part === `'$${termToHighlight}'`) {
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

  const errorMessage = block?.errors?.join(', ');

  return (
    <StyledRow onMouseEnter={() => setShowActions(true)}
               onMouseLeave={() => setShowActions(false)}
               $hovered={showActions}>
      <Col xs={9} md={10}>
        <Row>
          <Col xs={10} md={9}>
            <BlockTitle title={block?.step_title}>
              {negatable
              && <NegationButton bsStyle="primary" bsSize="xs" $negate={block?.negate} onClick={(e) => { e.target.blur(); onNegate(); }}>Not</NegationButton>}
              {highlightedOutput ? (
                highlightedRuleTitle(highlightedOutput, block?.step_title)
              ) : block?.step_title}
              {block?.errors?.length > 0 && (
                <ErrorMessage title={errorMessage}>{errorMessage}</ErrorMessage>
              )}
            </BlockTitle>
          </Col>
          {block?.outputvariable && (
            <Col xs={2} md={3}>
              <Label bsStyle="primary"
                     onMouseEnter={() => setHighlightedOutput(block.outputvariable)}
                     onMouseLeave={() => setHighlightedOutput(undefined)}>
                {`$${block.outputvariable}`}
              </Label>
              {returnTypeLabel && (
              <TypeLabel bsStyle="default">
                {returnTypeLabel}
              </TypeLabel>
              )}
            </Col>
          )}
        </Row>
      </Col>
      <Col xs={3} md={2} className="text-right">
        {showActions && type === 'condition' && (
          <ActionsContainer>
            <IconButton name="edit" onClick={onEdit} title="Edit" />
            <IconButton name="trash-alt" onClick={onDelete} title="Delete" />
          </ActionsContainer>
        )}
        {showActions && type === 'action' && (
          <ActionsContainer>
            <EditIconButton name="edit" onClick={onEdit} title="Edit" />
            <OverlayDropdownButton title={MORE_ACTIONS_TITLE}
                                   buttonTitle={MORE_ACTIONS_HOVER_TITLE}
                                   bsSize="xsmall"
                                   closeOnSelect={false}
                                   dropdownZIndex={1000}>
              {({ toggleDropdown }) => (
                <>
                  <MenuItem onClick={onEdit}>Edit</MenuItem>
                  <MenuItem onClick={() => {
                    onDuplicate();
                    toggleDropdown();
                    setShowActions(false);
                  }}>
                    Duplicate
                  </MenuItem>
                  <MenuItem divider />
                  <MenuItem onClick={() => {
                    onInsertAbove();
                    toggleDropdown();
                    setShowActions(false);
                  }}>
                    Insert above
                  </MenuItem>
                  <MenuItem onClick={() => {
                    onInsertBelow();
                    toggleDropdown();
                    setShowActions(false);
                  }}>
                    Insert below
                  </MenuItem>
                  <MenuItem divider />
                  <MenuItem onClick={onDelete}>Delete</MenuItem>
                </>
              )}
            </OverlayDropdownButton>
          </ActionsContainer>
        )}
      </Col>
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
