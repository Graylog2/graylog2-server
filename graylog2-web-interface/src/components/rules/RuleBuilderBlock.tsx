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

import { Button, Input } from 'components/bootstrap';
import { RuleBuilderSupportedTypes } from 'hooks/useRuleBuilder';
import type { RuleBlock, BlockDict, BlockFieldDict } from 'hooks/useRuleBuilder';

import Select from '../common/Select';

type Props = {
  type: 'condition' | 'action',
  blockDict: Array<BlockDict>,
  block: RuleBlock,
  addBlock: (type: string, block: RuleBlock) => void,
  updateBlock: (orderIndex: number, type: string, block: RuleBlock) => void,
  deleteBlock: (orderIndex: number, type: string,) => void,
};

const RuleBuilderBlock = ({ type, blockDict, block, addBlock, updateBlock, deleteBlock }: Props) => {
  const [currentBlockDict, setCurrentBlockDict] = useState<BlockDict>(undefined);
  const [editMode, setEditMode] = useState<boolean>(false);

  // Todo: add temp state
  // Todo: add save button

  useEffect(() => {
    if (block) { setCurrentBlockDict(blockDict.find(((b) => b.name === block.function))); }
  },
  [block, blockDict]);

  const buildParamField = (paramDict: BlockFieldDict) => {
    const paramValue = block?.parameters[paramDict.name];

    switch (paramDict.type) {
      case RuleBuilderSupportedTypes.String:
        return (<Input type="text" label={paramDict.name}>{paramValue || ''}</Input>);
      case RuleBuilderSupportedTypes.Number:
        return (<div>Number</div>);
      case RuleBuilderSupportedTypes.Boolean:
        return (<div>Boolean</div>);
      default:
        return null;
    }
  };

  const blockForm = () => (
    <>
      <Select id="block-select"
              name="block-select"
              placeholder={`Select a ${type}`}
              options={blockDict.map(({ name }) => ({ label: name, value: name }))}
              matchProp="label"
              onChange={(option: string) => {
                setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
              }}
              value={currentBlockDict?.name || ''} />
      {currentBlockDict.params.map((param) => buildParamField(param))}
      <Button onClick={() => setEditMode(false)}>Cancel</Button>
    </>
  );

  const blockDisplay = () => (
    <>
      <p>{currentBlockDict?.name || ''}</p>
      <Button onClick={() => setEditMode(true)}>Edit</Button>
    </>
  );

  const showForm = !block || editMode;

  return (
    <div>
      {showForm ? blockForm() : blockDisplay()}
    </div>
  );
};

export default RuleBuilderBlock;
