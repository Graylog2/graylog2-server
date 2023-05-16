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

import { Select } from 'components/common';
import { Button, Input } from 'components/bootstrap';
import { RuleBuilderSupportedTypes } from 'hooks/useRuleBuilder';
import type { BlockDict, BlockFieldDict, RuleBlock } from 'hooks/useRuleBuilder';
import type { BlockType } from 'components/rules/RuleBuilderBlock';

type Props = {
  block: RuleBlock,
  fieldValues: {[key: string]: any},
  handleFieldChange: (fieldName: string, fieldValue: any) => void,
  onAdd: () => void,
  onCancel: () => void,
  onSelect: (option: string) => void,
  onUpdate: () => void
  options: Array<{ label: string, value: any }>,
  selectedBlockDict: BlockDict,
  type: BlockType,
}

const RuleBlockForm = ({
  block,
  fieldValues,
  handleFieldChange,
  onAdd,
  onCancel,
  onSelect,
  onUpdate,
  options,
  selectedBlockDict,
  type,
}: Props) => {
  const buildParamField = (paramDict: BlockFieldDict) => {
    const paramValue = block?.params[paramDict.name];

    switch (paramDict.type) {
      case RuleBuilderSupportedTypes.String:
        return (<Input type="text" label={paramDict.name} onChange={(e) => handleFieldChange(paramDict.name, e.target.value)} value={fieldValues[paramDict.name] || paramValue || ''} />);
      case RuleBuilderSupportedTypes.Number:
        return (<div>Number</div>);
      case RuleBuilderSupportedTypes.Boolean:
        return (<div>Boolean</div>);
      default:
        return null;
    }
  };

  return (
    <>
      <Select id="block-select"
              name="block-select"
              placeholder={`Select a ${type}`}
              options={options}
              matchProp="label"
              onChange={onSelect}
              value={selectedBlockDict?.name || ''} />
      {selectedBlockDict?.params.map((param) => buildParamField(param))}
      {block ? <Button onClick={onUpdate}>Update</Button> : <Button onClick={onAdd}>Add</Button>}
      {(block || selectedBlockDict) && <Button onClick={onCancel}>Cancel</Button>}
    </>
  );
};

export default RuleBlockForm;
