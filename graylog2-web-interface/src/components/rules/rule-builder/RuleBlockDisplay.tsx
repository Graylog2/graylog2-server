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

import { Button, Col, Label, Row } from 'components/bootstrap';
import { IconButton, Icon } from 'components/common';

import Errors from './Errors';
import type { RuleBlock } from './types';
import { ruleBlockPropType } from './types';

type Props = {
  block: RuleBlock,
  negatable?: boolean,
  onDelete: () => void,
  onEdit: () => void,
  onNegate: () => void,
}

const BlockInfo = styled(Row)(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const OutputVariable = styled.p(({ theme }) => css`
  margin-left: ${theme.spacings.xxs};
  margin-top: ${theme.spacings.sm};
  margin-bottom: 0;
`);

const OutputIcon = styled(Icon)(({ theme }) => css`
  margin-right: ${theme.spacings.sm};
`);

const NegationButton = styled(Button).attrs(({ negate }: { negate: boolean }) => ({
  negate,
}))(({ negate, theme }) => css`
  opacity: ${negate ? '1' : '0.3'};
  margin-right: ${theme.spacings.sm};
`);

const RuleBlockDisplay = ({ block, negatable, onEdit, onDelete, onNegate } : Props) => (
  <Row>
    <Col xs={9} md={10}>
      <BlockInfo>
        <Col md={12}>
          <h3>
            {negatable
              && <NegationButton bsStyle="primary" negate={block?.negate ? 1 : 0} onClick={(e) => { e.target.blur(); onNegate(); }}>Not</NegationButton>}
            {block?.step_title}
          </h3>
          {block?.outputvariable && (
            <OutputVariable>
              <OutputIcon name="level-up-alt" rotation={90} />
              <Label bsStyle="primary">
                {`$${block?.outputvariable}`}
              </Label>
            </OutputVariable>
          )}
        </Col>
      </BlockInfo>
      <Errors objectWithErrors={block} />
    </Col>
    <Col xs={3} md={2} className="text-right">
      <IconButton name="edit" onClick={onEdit} title="Edit" />
      <IconButton name="trash-alt" onClick={onDelete} title="Delete" />
    </Col>
  </Row>
);

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
