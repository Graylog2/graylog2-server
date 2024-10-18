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
import * as React from 'react';
import { useState } from 'react';
import styled, { css } from 'styled-components';
import { useFormikContext } from 'formik';
import ObjectID from 'bson-objectid';

import Errors from 'components/rules/rule-builder/Errors';
import { ConfirmDialog } from 'components/common';
import type { BlockType, RuleBlock } from 'components/rules/rule-builder/types';
import RuleBuilderBlock from 'components/rules/rule-builder/RuleBuilderBlock';
import { Panel, Radio } from 'components/bootstrap';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import useStreamOutputRuleBuilder, { fetchValidateRule } from 'components/streams/hooks/useStreamOutputRuleBuilder';

type Props = {
  type: BlockType,
};

const StyledPanel = styled(Panel)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 0;
  box-shadow: none;
  margin-bottom: 0;
`);

const StyledPanelHeading = styled(Panel.Heading)(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  background-color: ${theme.colors.table.row.backgroundAlt} !important;
  border: 0;
`);

const WhenOperator = styled.div(({ theme }) => css`
  display: flex;

  .radio {
    margin: 0 ${theme.spacings.xs};
  }
`);

const StyledPanelBody = styled(Panel.Body)`
  border: 0;
  padding: 0;
`;

const FilterRulesFields = ({ type }: Props) => {
  const { values, setFieldValue, setValues } = useFormikContext<StreamOutputFilterRule>();
  const [blockToDelete, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);
  const newConditionBlockIndex = values.rule?.conditions?.length;
  const { conditions } = useStreamOutputRuleBuilder();

  const validateAndUpdateFormValues = (ruleToValidate: StreamOutputFilterRule) => fetchValidateRule(ruleToValidate).then((ruleValidated) => {
    setValues({ ...values, rule: ruleValidated.rule });
  }).catch(() => setValues(ruleToValidate));

  const addBlock = async (blockType: BlockType, block: RuleBlock) => {
    if (blockType === 'condition') {
      const ruleToValidate = { ...values, rule: { ...values.rule, conditions: [...(values.rule?.conditions || []), { ...block, id: new ObjectID().toString() }] } };
      validateAndUpdateFormValues(ruleToValidate);
    }
  };

  const updateBlock = async (orderIndex: number, blockType: string, block: RuleBlock) => {
    if (blockType === 'condition') {
      const currentConditions = [...values.rule.conditions];
      currentConditions[orderIndex] = block;
      const ruleToValidate = { ...values, rule: { ...values.rule, conditions: currentConditions } };
      validateAndUpdateFormValues(ruleToValidate);
    }
  };

  const deleteBlock = (orderIndex: number, blockType: BlockType) => {
    if (blockType === 'condition') {
      const currentConditions = [...values.rule.conditions];
      currentConditions.splice(orderIndex, 1);
      const ruleToValidate = { ...values, rule: { ...values.rule, conditions: currentConditions } };
      validateAndUpdateFormValues(ruleToValidate);
    }
  };

  return (
    <StyledPanel expanded>
      <StyledPanelHeading>
        <Panel.Title toggle>
          {type === 'condition' ? 'When' : 'Then'}
        </Panel.Title>
        {type === 'condition' && (
        <WhenOperator>
          <Radio name="operator"
                 checked={values.rule?.operator === 'AND'}
                 onChange={() => setFieldValue('rule.operator', 'AND')}>
            and
          </Radio>
          <Radio name="operator"
                 checked={values.rule?.operator === 'OR'}
                 onChange={() => setFieldValue('rule.operator', 'OR')}>
            or
          </Radio>
        </WhenOperator>
        )}
      </StyledPanelHeading>
      <Panel.Collapse>
        <StyledPanelBody>
          {values.rule?.[`${type}s`]?.map((item, index) => (

            <RuleBuilderBlock key={item.id}
                              blockDict={conditions || []}
                              block={item}
                              order={index}
                              type={type}
                              addBlock={addBlock}
                              updateBlock={updateBlock}
                              deleteBlock={() => setBlockToDelete({ orderIndex: index, type })} />
          ))}
          <RuleBuilderBlock blockDict={conditions || []}
                            order={newConditionBlockIndex}
                            type={type}
                            addBlock={addBlock}
                            updateBlock={updateBlock}
                            deleteBlock={() => setBlockToDelete({
                              orderIndex: newConditionBlockIndex,
                              type,
                            })} />
        </StyledPanelBody>
      </Panel.Collapse>
      {blockToDelete && (
      <ConfirmDialog title={`Delete ${blockToDelete.type}`}
                     show
                     onConfirm={() => {
                       deleteBlock(blockToDelete.orderIndex, blockToDelete.type);
                       setBlockToDelete(null);
                     }}
                     onCancel={() => setBlockToDelete(null)}>
        <>Are you sure you want to delete <strong>{blockToDelete.type} N° {blockToDelete.orderIndex + 1}</strong>?</>
      </ConfirmDialog>
      )}
      <Errors objectWithErrors={values.rule} />
    </StyledPanel>

  );
};

export default FilterRulesFields;
