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
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';

import type { RuleBlock, BlockType, BlockDict, OutputVariables } from './types';
import { ruleBlockPropType, blockDictPropType, outputVariablesPropType, RuleBuilderTypes } from './types';
import { getDictForFunction } from './helpers';

const BlockContainer = styled.div(({ theme }) => css`
  padding-top: ${theme.spacings.xxs};
`);

type Props = {
  type: BlockType,
  blockDict: Array<BlockDict>,
  block?: RuleBlock,
  order: number,
  outputVariableList?: OutputVariables,
  addBlock: (type: string, block: RuleBlock, orderIndex?: number) => void,
  updateBlock: (orderIndex: number, type: string, block: RuleBlock) => void,
  deleteBlock: (orderIndex: number, type: string) => void,
};

const RuleBuilderBlock = ({
  type,
  blockDict,
  block,
  order,
  outputVariableList,
  addBlock,
  updateBlock,
  deleteBlock,
}: Props) => {
  const [currentBlockDict, setCurrentBlockDict] = useState<BlockDict>(undefined);
  const [editMode, setEditMode] = useState<boolean>(false);

  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    if (block) {
      setCurrentBlockDict(getDictForFunction(blockDict, block.function));
    }
  },
  [block, blockDict]);

  const buildBlockData = (
    newData: { newFunctionName?: string, newParams?: object, toggleNegate?: boolean },
  ) => {
    const defaultParameters = { newFunctionName: currentBlockDict.name, newParams: {}, toggleNegate: false };
    const { newFunctionName, newParams, toggleNegate } = { ...defaultParameters, ...newData };

    const defaultBlock = { function: newFunctionName, params: {} };

    let newBlock;

    if (block && newFunctionName === block.function) {
      newBlock = block;
    } else {
      newBlock = defaultBlock;
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
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `delete-${type}-button`,
    });

    deleteBlock(order, type);
  };

  const onEdit = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `edit-${type}-button`,
    });

    setEditMode(true);
  };

  const onNegate = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `negate-${type}-button`,
    });

    updateBlock(order, type, buildBlockData({ toggleNegate: true }));
  };

  const onDuplicate = async () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `duplicate-${type}`,
    });

    const duplicatedBlock = { ...block, outputvariable: null };
    addBlock(type, duplicatedBlock, order + 1);
  };

  const onInsertAbove = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `insert-above-${type}`,
    });
  };

  const onInsertBelow = () => {
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `insert-below-${type}`,
    });
  };

  const onUpdate = (params: { [key: string]: any }, functionName: string) => {
    updateBlock(order, type, buildBlockData({ newFunctionName: functionName, newParams: params }));
    setEditMode(false);
  };

  const onSelect = (option: string) => {
    setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
  };

  const isBlockNegatable = (): boolean => (
    type === 'condition' && (
      currentBlockDict?.return_type === RuleBuilderTypes.Boolean
      || currentBlockDict?.rule_builder_function_group === 'Boolean Functions'
    )
  );

  const options = blockDict.map(({ name, description, rule_builder_name }) => ({ label: rule_builder_name, value: name, description: description }));

  const showForm = !block || editMode;

  return (
    <BlockContainer>
      {showForm ? (
        <RuleBlockForm existingBlock={block}
                       onAdd={onAdd}
                       onCancel={onCancel}
                       onUpdate={onUpdate}
                       onSelect={onSelect}
                       order={order}
                       options={options}
                       outputVariableList={outputVariableList}
                       selectedBlockDict={currentBlockDict}
                       type={type} />
      ) : (
        <RuleBlockDisplay block={block}
                          onDelete={onDelete}
                          onEdit={onEdit}
                          onNegate={onNegate}
                          onDuplicate={onDuplicate}
                          onInsertAbove={onInsertAbove}
                          onInsertBelow={onInsertBelow}
                          returnType={currentBlockDict?.return_type}
                          negatable={isBlockNegatable()}
                          type={type} />
      )}
    </BlockContainer>
  );
};

RuleBuilderBlock.propTypes = {
  type: PropTypes.oneOf(['action', 'condition']).isRequired,
  blockDict: PropTypes.arrayOf(blockDictPropType).isRequired,
  block: ruleBlockPropType,
  order: PropTypes.number.isRequired,
  outputVariableList: outputVariablesPropType,
  addBlock: PropTypes.func.isRequired,
  updateBlock: PropTypes.func.isRequired,
  deleteBlock: PropTypes.func.isRequired,
};

RuleBuilderBlock.defaultProps = {
  block: undefined,
  outputVariableList: undefined,
};

export default RuleBuilderBlock;
