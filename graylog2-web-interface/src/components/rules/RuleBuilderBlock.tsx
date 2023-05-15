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

import type { Block } from './RuleBuilder';

import Select from '../common/Select';

type Props = {
  type: 'condition'|'action',
  blockDict: object[],
  block: Block,
  addBlock: (type: string, block: object) => void,
  updateBlock: (orderIndex: number, type: string, block: object) => void,
  deleteBlock: (orderIndex: number, type: string,) => void,
};

const RuleBuilderBlock = ({ type, blockDict, block, addBlock, updateBlock, deleteBlock }: Props) => {
  const [currentBlock, setCurrentBlock] = useState<string>(undefined);

  return (
    <div>
      <Select id="block-select"
              name="block-select"
              placeholder={`Select a ${type}`}
              options={blockDict.map(({ name }) => ({ label: name, value: name }))}
              matchProp="label"
              onChange={(option: string) => {
                setCurrentBlock(option);
              }}
              value={currentBlock} />
    </div>
  );
};

export default RuleBuilderBlock;
