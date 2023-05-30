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
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import RuleBlockDisplay from 'components/rules/rule-builder/RuleBlockDisplay';
import RuleBlockForm from 'components/rules/rule-builder/RuleBlockForm';

import type { RuleBlock, BlockType, BlockDict } from './types';
import { ruleBlockPropType, blockDictPropType, RuleBuilderTypes } from './types';
import { getDictForFunction } from './helpers';

const BlockContainer = styled.div.attrs(({ hasErrors }: { hasErrors: boolean }) => ({
  hasErrors,
}))(({ hasErrors, theme }) => css`
  border-radius: 4px;
  border-color: ${hasErrors ? theme.colors.variant.lighter.danger : theme.colors.variant.lighter.default};
  padding: ${theme.spacings.md};
  margin-bottom: ${theme.spacings.md};
`);

type Props = {
  type: BlockType,
  blockDict: Array<BlockDict>,
  block?: RuleBlock,
  order: number,
  previousOutputPresent?: boolean,
  addBlock: (type: string, block: RuleBlock) => void,
  updateBlock: (orderIndex: number, type: string, block: RuleBlock) => void,
  deleteBlock: (orderIndex: number, type: string,) => void,
};

const RuleBuilderBlock = ({ type, blockDict, block, order, previousOutputPresent, addBlock, updateBlock, deleteBlock }: Props) => {
  const [currentBlockDict, setCurrentBlockDict] = useState<BlockDict>(undefined);
  const [editMode, setEditMode] = useState<boolean>(false);

  useEffect(() => {
    if (block) { setCurrentBlockDict(blockDict.find(((b) => b.name === block.function))); }
  },
  [block, blockDict]);

  const buildBlockData = (newData : {newParams?: object, toggleNegate?: boolean} = { newParams: {}, toggleNegate: false }) => {
    const { newParams, toggleNegate } = newData;

    let newBlock;

    if (block) {
      newBlock = block;
    } else {
      newBlock = { function: currentBlockDict.name, params: {} };
    }

    if (toggleNegate) {
      newBlock.negate = !newBlock.negate;
    }

    return { ...newBlock, params: { ...newBlock.params, ...newParams } };
  };

  const resetBlock = () => {
    if (block) {
      setCurrentBlockDict(blockDict.find(((b) => b.name === block.function)));
    } else {
      setCurrentBlockDict(undefined);
    }
  };

  const onCancel = () => {
    setEditMode(false);
    resetBlock();
  };

  const onAdd = (paramsToAdd) => {
    addBlock(type, buildBlockData({ newParams: paramsToAdd }));

    onCancel();
  };

  const onDelete = () => {
    deleteBlock(order, type);
  };

  const onEdit = () => {
    setEditMode(true);
  };

  const onNegate = () => {
    updateBlock(order, type, buildBlockData({ toggleNegate: true }));
  };

  const onUpdate = (params) => {
    updateBlock(order, type, buildBlockData({ newParams: params }));
    setEditMode(false);
  };

  const onSelect = (option: string) => {
    setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
  };

  const isBlockNegatable = () : boolean => (
    getDictForFunction(blockDict, block.function).return_type === RuleBuilderTypes.Boolean
  );

  const options = blockDict.map(({ name }) => ({ label: name, value: name }));

  const showForm = !block || editMode;

  return (
    <BlockContainer className="content" hasErrors={block?.errors?.length > 0}>
      {showForm ? (
        <RuleBlockForm existingBlock={block}
                       onAdd={onAdd}
                       onCancel={onCancel}
                       onUpdate={onUpdate}
                       onSelect={onSelect}
                       order={order}
                       options={options}
                       previousOutputPresent={previousOutputPresent}
                       selectedBlockDict={currentBlockDict}
                       type={type} />
      ) : (
        <RuleBlockDisplay block={block}
                          onDelete={onDelete}
                          onEdit={onEdit}
                          onNegate={onNegate}
                          negatable={isBlockNegatable()} />
      )}
    </BlockContainer>
  );
};

RuleBuilderBlock.propTypes = {
  type: PropTypes.oneOf(['action', 'condition']).isRequired,
  blockDict: PropTypes.arrayOf(blockDictPropType).isRequired,
  block: ruleBlockPropType,
  order: PropTypes.number.isRequired,
  previousOutputPresent: PropTypes.bool,
  addBlock: PropTypes.func.isRequired,
  updateBlock: PropTypes.func.isRequired,
  deleteBlock: PropTypes.func.isRequired,
};

RuleBuilderBlock.defaultProps = {
  block: undefined,
  previousOutputPresent: false,
};

export default RuleBuilderBlock;
