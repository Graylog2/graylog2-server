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

import type { BlockType, RuleBlock } from 'components/rules/rule-builder/types';
import RuleBuilderBlock from 'components/rules/rule-builder/RuleBuilderBlock';
import { Panel, Radio } from 'components/bootstrap';

import type { StreamOutputFilterRule } from './Types';

import useStreamOutputRuleBuilder, { fetchValidateRule } from '../../hooks/useStreamOutputRuleBuilder';

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

const WhenOperator = styled.div`
  display: flex;

  .radio {
    margin: 0 8px;
  }
`;

const StyledPanelBody = styled(Panel.Body)`
  border: 0;
  padding: 0;
`;

const FilterRulesFields = ({ type }: Props) => {
  const { values, setFieldValue, setValues } = useFormikContext<StreamOutputFilterRule>();
  const validateAndUpdateFormValues = (ruleToValidate: StreamOutputFilterRule) => fetchValidateRule(ruleToValidate).then((ruleValidated) => {
    setValues({ ...values, rule: ruleValidated.rule });
  }).catch(() => setValues(ruleToValidate));

  const addBlock = async (blockType: BlockType, block: RuleBlock) => {
    if (blockType === 'condition') {
      const ruleToValidate = { ...values, rule: { ...values.rule, conditions: [...(values.rule?.conditions || []), { ...block, id: new ObjectID().toString() }] } };
      validateAndUpdateFormValues(ruleToValidate);
    }
  };

  const updateBlock = async (/* orderIndex: number, type: string, block: RuleBlock */) => {};
  const [, setBlockToDelete] = useState<{ orderIndex: number, type: BlockType } | null>(null);
  const newConditionBlockIndex = values.rule?.conditions.length;
  const { conditions } = useStreamOutputRuleBuilder();

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
                 onChange={() => setFieldValue('operator', 'AND')}>
            and
          </Radio>
          <Radio name="operator"
                 checked={values.rule?.operator === 'OR'}
                 onChange={() => setFieldValue('operator', 'OR')}>
            or
          </Radio>
        </WhenOperator>
        )}
      </StyledPanelHeading>
      <Panel.Collapse>
        <StyledPanelBody>
          {values.rule?.[`${type}s`]?.map((item, index) => (
            // eslint-disable-next-line react/no-array-index-key
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
    </StyledPanel>

  );
};

export default FilterRulesFields;
