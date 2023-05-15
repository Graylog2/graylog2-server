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

import { Input } from 'components/bootstrap';
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

  return (
    <div>
      <Select id="block-select"
              name="block-select"
              placeholder={`Select a ${type}`}
              options={blockDict.map(({ name }) => ({ label: name, value: name }))}
              matchProp="label"
              onChange={(option: string) => {
                setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
              }}
              value={currentBlockDict?.name || ''} />

      {currentBlockDict && (
        <>
          <p>{currentBlockDict.name}</p>
          {currentBlockDict.params.map((param) => buildParamField(param))}
        </>
      )}
    </div>
  );
};

export default RuleBuilderBlock;
