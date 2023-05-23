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
import { ruleBlockPropType, blockDictPropType } from './types';

const BlockContainer = styled.div(({ theme }) => css`
  border-radius: 4px;
  padding: ${theme.spacings.md};
  margin-bottom: ${theme.spacings.md};
`);

type Props = {
  type: BlockType,
  blockDict: Array<BlockDict>,
  block?: RuleBlock,
  order: number,
  addBlock: (type: string, block: RuleBlock) => void,
  updateBlock: (orderIndex: number, type: string, block: RuleBlock) => void,
  deleteBlock: (orderIndex: number, type: string,) => void,
};

const RuleBuilderBlock = ({ type, blockDict, block, order, addBlock, updateBlock, deleteBlock }: Props) => {
  const [currentBlockDict, setCurrentBlockDict] = useState<BlockDict>(undefined);
  const [editMode, setEditMode] = useState<boolean>(false);

  useEffect(() => {
    if (block) { setCurrentBlockDict(blockDict.find(((b) => b.name === block.function))); }
  },
  [block, blockDict]);

  const buildBlockData = (newParams) => {
    if (block) {
      return { ...block, params: { ...block.params, ...newParams } };
    }

    return { function: currentBlockDict.name, params: newParams };
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
    addBlock(type, buildBlockData(paramsToAdd));

    onCancel();
  };

  const onDelete = () => {
    deleteBlock(order, type);
  };

  const onEdit = () => {
    setEditMode(true);
  };

  const onUpdate = (params) => {
    updateBlock(order, type, buildBlockData(params));
    setEditMode(false);
  };

  const onSelect = (option: string) => {
    setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
  };

  const options = blockDict.map(({ name }) => ({ label: name, value: name }));

  const showForm = !block || editMode;

  return (
    <BlockContainer className="content">
      {showForm ? (
        <RuleBlockForm existingBlock={block}
                       onAdd={onAdd}
                       onCancel={onCancel}
                       onUpdate={onUpdate}
                       onSelect={onSelect}
                       options={options}
                       selectedBlockDict={currentBlockDict}
                       type={type} />
      ) : (
        <RuleBlockDisplay block={block}
                          blockDict={currentBlockDict}
                          onDelete={onDelete}
                          onEdit={onEdit} />
      )}
    </BlockContainer>
  );
};

RuleBuilderBlock.propTypes = {
  type: PropTypes.oneOf(['action', 'condition']).isRequired,
  blockDict: PropTypes.arrayOf(blockDictPropType).isRequired,
  block: ruleBlockPropType,
  order: PropTypes.number.isRequired,
  addBlock: PropTypes.func.isRequired,
  updateBlock: PropTypes.func.isRequired,
  deleteBlock: PropTypes.func.isRequired,
};

RuleBuilderBlock.defaultProps = {
  block: undefined,
};

export default RuleBuilderBlock;
