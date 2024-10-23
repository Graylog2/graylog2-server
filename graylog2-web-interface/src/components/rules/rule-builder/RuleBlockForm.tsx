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
import { Formik } from 'formik';
import styled, { css } from 'styled-components';

import { FormSubmit, Icon, OverlayTrigger, Select, NestedForm } from 'components/common';
import { Button, Col, Row } from 'components/bootstrap';
import RuleBlockFormField from 'components/rules/rule-builder/RuleBlockFormField';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import Errors from './Errors';
import { RuleBuilderTypes } from './types';
import type { BlockType, RuleBlock, BlockDict, BlockFieldDict, OutputVariables } from './types';

import RuleHelperTable from '../rule-helper/RulerHelperTable';

type Option = { label: string, value: any, description?: string | null };

type Props = {
  existingBlock?: RuleBlock,
  onAdd: (values: { [key: string]: any }) => void,
  onCancel: () => void,
  onSelect: (option: string) => void,
  onUpdate: (values: { [key: string]: any }, functionName: string) => void,
  options: Array<Option>,
  order: number,
  outputVariableList?: OutputVariables,
  selectedBlockDict?: BlockDict,
  type: BlockType,
}

const BlockDescription = styled.p(({ theme }) => css`
  color: ${theme.colors.gray[50]};
`);

const SelectedBlock = styled.div(({ theme }) => css`
  border-left: 1px solid ${theme.colors.gray['90']};
  border-right: 1px solid ${theme.colors.gray['90']};
  border-bottom: 1px solid ${theme.colors.gray['90']};
  border-radius: 0 0 10px 10px;
  padding: ${theme.spacings.md};
`);

const SelectedBlockInfo = styled(Row)(({ theme }) => css`
  margin-bottom: ${theme.spacings.md};
`);

const OptionTitle = styled.p(({ theme }) => css`
  margin-bottom: ${theme.spacings.xxs};
`);

const SelectRow = styled(Row)(({ theme }) => css`
  margin-top: ${theme.spacings.xxs};
  margin-bottom: ${theme.spacings.xxs};
`);

const OptionDescription = styled.p<{ $isSelected: boolean }>(({ theme, $isSelected }) => css`
  color: ${$isSelected ? theme.colors.gray[90] : theme.colors.gray[50]};
  margin-bottom: ${theme.spacings.xxs};
  font-size: 0.75rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
`);

const RuleBlockForm = ({
  existingBlock,
  onAdd,
  onCancel,
  onSelect,
  onUpdate,
  options,
  order,
  outputVariableList,
  selectedBlockDict,
  type,
}: Props) => {
  const [initialValues, setInitialValues] = useState<{}>({});

  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    const newInitialValues = {};

    if (selectedBlockDict) {
      selectedBlockDict.params.forEach((param: BlockFieldDict) => {
        const initialBlockValue = existingBlock?.function === selectedBlockDict.name ? existingBlock?.params[param.name] : undefined;

        if (
          initialBlockValue
          || ((param.type === RuleBuilderTypes.Boolean) && (typeof initialBlockValue === 'boolean'))
        ) {
          newInitialValues[param.name] = initialBlockValue;
        } else {
          newInitialValues[param.name] = param.default_value;
        }
      });
    }

    setInitialValues(newInitialValues);
  }, [selectedBlockDict, existingBlock]);

  const handleChange = (option: string, resetForm: () => void) => {
    sendTelemetry(
      type === 'condition'
        ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.NEW_CONDITION_SELECTED
        : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.NEW_ACTION_SELECTED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'pipeline-rule-builder',
        app_action_value: `select-${type}`,
        event_details: { option },
      });

    resetForm();
    onSelect(option);
  };

  const resetField = (fieldName: string, setFieldValue: (field: string, value: any, shouldValidate?: boolean) => void) => {
    setFieldValue(fieldName, null);
  };

  const onSubmit = (values: { [key: string]: any }) => {
    if (existingBlock) {
      sendTelemetry(
        type === 'condition'
          ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.UPDATE_CONDITION_CLICKED
          : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.UPDATE_ACTION_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'pipeline-rule-builder',
          app_action_value: `update-${type}-button`,
        });

      onUpdate(values, selectedBlockDict?.name);
    } else {
      sendTelemetry(
        type === 'condition'
          ? TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.ADD_CONDITION_CLICKED
          : TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.ADD_ACTION_CLICKED, {
          app_pathname: getPathnameWithoutId(pathname),
          app_section: 'pipeline-rule-builder',
          app_action_value: `add-${type}-button`,
        });

      onAdd(values);
    }
  };

  const optionRenderer = (option: Option, isSelected: boolean) => (
    <>
      <OptionTitle>{option.label}</OptionTitle>
      {option.description && (<OptionDescription $isSelected={isSelected}>{option.description}</OptionDescription>)}
    </>
  );

  return (
    <Row>
      <Col md={12}>
        <Formik enableReinitialize onSubmit={onSubmit} initialValues={initialValues}>
          {({ resetForm, setFieldValue, isValid }) => (
            <NestedForm>
              <SelectRow>
                <Col md={12}>
                  <Select id={`existingBlock-select-${type}`}
                          name={`existingBlock-select-${type}`}
                          placeholder={`Add ${type}`}
                          options={options}
                          optionRenderer={optionRenderer}
                          clearable={false}
                          matchProp="label"
                          autoFocus
                          onChange={(option: string) => handleChange(option, resetForm)}
                          value={selectedBlockDict?.name || ''} />
                </Col>
              </SelectRow>

              {selectedBlockDict && (
                <SelectedBlock>
                  <SelectedBlockInfo>
                    <Col md={12}>
                      <h5>
                        {existingBlock?.step_title || selectedBlockDict.rule_builder_name}
                        <OverlayTrigger trigger="click"
                                        rootClose
                                        placement="right"
                                        title="Function Syntax Help"
                                        width={700}
                                        overlay={<RuleHelperTable entries={[selectedBlockDict]} expanded={{ [selectedBlockDict.name]: true }} />}>
                          <Button bsStyle="link">
                            <Icon name="help"
                                  title="Function Syntax Help"
                                  data-testid="funcSyntaxHelpIcon" />
                          </Button>
                        </OverlayTrigger>
                      </h5>
                      <BlockDescription>{selectedBlockDict.description}</BlockDescription>
                    </Col>
                  </SelectedBlockInfo>

                  {selectedBlockDict.params.map((param) => (
                    <Row key={`${order}_${param.name}`}>
                      <RuleBlockFormField param={param}
                                          functionName={selectedBlockDict.name}
                                          order={order}
                                          blockId={existingBlock?.id}
                                          outputVariableList={outputVariableList}
                                          blockType={type}
                                          resetField={(fieldName) => resetField(fieldName, setFieldValue)} />
                    </Row>
                  ),
                  )}

                  <Errors objectWithErrors={existingBlock} />
                  <FormSubmit bsSize="small"
                              disabledSubmit={!isValid}
                              submitButtonText={existingBlock ? 'Update' : 'Add'}
                              submitButtonType="submit"
                              onCancel={() => {
                                resetForm();
                                onCancel();
                              }} />

                </SelectedBlock>
              )}
            </NestedForm>
          )}
        </Formik>
      </Col>
    </Row>
  );
};

export default RuleBlockForm;
