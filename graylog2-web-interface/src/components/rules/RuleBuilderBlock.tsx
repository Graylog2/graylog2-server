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

import type { RuleBlock, BlockDict } from 'hooks/useRuleBuilder';
import RuleBlockDisplay from 'components/rules/rule-builder/RuleBlockDisplay';
import RuleBlockForm from 'components/rules/rule-builder/RuleBlockForm';

export type BlockType = 'condition' | 'action'

type Props = {
  type: BlockType,
  blockDict: Array<BlockDict>,
  block: RuleBlock,
  order: number,
  addBlock: (type: string, block: RuleBlock) => void,
  updateBlock: (orderIndex: number, type: string, block: RuleBlock) => void,
  deleteBlock: (orderIndex: number, type: string,) => void,
};

const RuleBuilderBlock = ({ type, blockDict, block, order, addBlock, updateBlock, deleteBlock }: Props) => {
  const [currentBlockDict, setCurrentBlockDict] = useState<BlockDict>(undefined);
  const [editMode, setEditMode] = useState<boolean>(false);
  const [fieldValues, setFieldValues] = useState<{[key: string]: any}>({});

  useEffect(() => {
    if (block) { setCurrentBlockDict(blockDict.find(((b) => b.name === block.function))); }
  },
  [block, blockDict]);

  const buildBlockData = () => {
    if (block) {
      return { ...block, params: { ...block.params, ...fieldValues } };
    }

    return { function: currentBlockDict.name, params: fieldValues };
  };

  const resetBlock = () => {
    if (block) {
      setCurrentBlockDict(blockDict.find(((b) => b.name === block.function)));
    } else {
      setCurrentBlockDict(undefined);
    }

    setFieldValues({});
  };

  const handleFieldChange = (fieldName, fieldValue) => { setFieldValues({ ...fieldValues, [fieldName]: fieldValue }); };

  const onAdd = () => {
    addBlock(type, buildBlockData());

    setEditMode(false);
  };

  const onCancel = () => {
    setEditMode(false);
    resetBlock();
  };

  const onDelete = () => {
    deleteBlock(order, type);
  };

  const onEdit = () => {
    setEditMode(true);
  };

  const onUpdate = () => {
    updateBlock(order, type, buildBlockData());
    setEditMode(false);
  };

  const onSelect = (option: string) => {
    setFieldValues({});
    setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
  };

  const options = blockDict.map(({ name }) => ({ label: name, value: name }));

  const showForm = !block || editMode;

  return (
    <div>
      {showForm ? (
        <RuleBlockForm block={block}
                       fieldValues={fieldValues}
                       handleFieldChange={handleFieldChange}
                       onAdd={onAdd}
                       onCancel={onCancel}
                       onUpdate={onUpdate}
                       onSelect={onSelect}
                       options={options}
                       selectedBlockDict={currentBlockDict}
                       type={type} />
      ) : (
        <RuleBlockDisplay blockDict={currentBlockDict}
                          onDelete={onDelete}
                          onEdit={onEdit} />
      )}
    </div>
  );
};

export default RuleBuilderBlock;
