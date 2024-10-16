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
import styled, { css } from 'styled-components';

import RuleBlockDisplay from 'components/rules/rule-builder/RuleBlockDisplay';
import RuleBlockForm from 'components/rules/rule-builder/RuleBlockForm';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { Modal } from 'components/bootstrap';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { RuleBlock, BlockType, BlockDict, OutputVariables } from './types';
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
  const [insertMode, setInsertMode] = useState<'above'|'below'|undefined>(undefined);
  const [insertBlockDict, setInsertBlockDict] = useState<BlockDict>(undefined);

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
    setEditMode(false);

    if (block) {
      setCurrentBlockDict(blockDict.find(((b) => b.name === block.function)));
    } else {
      setCurrentBlockDict(undefined);
    }
  };

  const resetInsertBlock = () => {
    setInsertMode(undefined);
    setInsertBlockDict(undefined);
  };

  const onCancel = () => {
    resetBlock();
    resetInsertBlock();
  };

  const onAdd = (paramsToAdd) => {
    addBlock(type, buildBlockData({ newParams: paramsToAdd }));

    onCancel();
  };

  const onDelete = () => {
    sendTelemetry(
      type === 'condition'
        ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.DELETE_CONDITION_CLICKED
        : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.DELETE_ACTION_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rule-builder',
        app_action_value: `delete-${type}-button`,
      });

    deleteBlock(order, type);
  };

  const onEdit = () => {
    sendTelemetry(
      type === 'condition'
        ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.EDIT_CONDITION_CLICKED
        : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.EDIT_ACTION_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rule-builder',
        app_action_value: `edit-${type}-button`,
      });

    setEditMode(true);
  };

  const onNegate = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.NEGATE_CONDITION_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `negate-${type}-button`,
    });

    updateBlock(order, type, buildBlockData({ toggleNegate: true }));
  };

  const onDuplicate = async () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.DUPLICATE_ACTION_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `duplicate-${type}`,
    });

    const duplicatedBlock = { ...block, outputvariable: null };
    addBlock(type, duplicatedBlock, order + 1);
  };

  const onInsert = (paramsToAdd) => {
    if (insertMode) {
      const newBlock: RuleBlock = { id: '', function: insertBlockDict.name, params: paramsToAdd, outputvariable: null };

      addBlock(type, newBlock, insertMode === 'above' ? order : order + 1);
    }

    onCancel();
  };

  const onInsertAbove = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.INSERT_ABOVE_ACTION_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `insert-above-${type}`,
    });

    setInsertMode('above');
  };

  const onInsertBelow = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.INSERT_BELOW_ACTION_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `insert-below-${type}`,
    });

    setInsertMode('below');
  };

  const onUpdate = (params: { [key: string]: any }, functionName: string) => {
    updateBlock(order, type, buildBlockData({ newFunctionName: functionName, newParams: params }));

    onCancel();
  };

  const onSelect = (option: string) => {
    setCurrentBlockDict(blockDict.find(((b) => b.name === option)));
  };

  const onInsertSelect = (option: string) => {
    setInsertBlockDict(blockDict.find(((b) => b.name === option)));
  };

  const isBlockNegatable = (): boolean => type === 'condition';

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
        <>
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
          {Boolean(insertMode) && (
            <Modal show
                   title="insert rule action"
                   bsSize="lg"
                   enforceFocus
                   onHide={resetInsertBlock}>
              <Modal.Header closeButton>
                <Modal.Title>Insert new action {insertMode} action N°{order + 1}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <RuleBlockForm onAdd={onInsert}
                               onCancel={onCancel}
                               onUpdate={onUpdate}
                               onSelect={onInsertSelect}
                               order={insertMode === 'above' ? order : order + 1}
                               options={options}
                               outputVariableList={outputVariableList}
                               selectedBlockDict={insertBlockDict}
                               type={type} />
              </Modal.Body>
            </Modal>
          )}
        </>
      )}
    </BlockContainer>
  );
};

export default RuleBuilderBlock;
