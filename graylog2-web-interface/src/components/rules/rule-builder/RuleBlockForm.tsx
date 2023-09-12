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
import { Formik, Form } from 'formik';
import styled, { css } from 'styled-components';

import { FormSubmit, Icon, OverlayTrigger, Select } from 'components/common';
import { Button, Col, Popover, Row } from 'components/bootstrap';
import RuleBlockFormField from 'components/rules/rule-builder/RuleBlockFormField';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import Errors from './Errors';
import { ruleBlockPropType, blockDictPropType, outputVariablesPropType, RuleBuilderTypes } from './types';
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

const FormTitle = styled.h3(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

const BlockTitle = styled.h3(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const BlockDescription = styled.p(({ theme }) => css`
  color: ${theme.colors.gray[50]};
`);

const SelectedBlock = styled.div(({ theme }) => css`
  border-left: 1px solid ${theme.colors.input.border};
  border-right: 1px solid ${theme.colors.input.border};
  border-bottom: 1px solid ${theme.colors.input.border};
  padding: ${theme.spacings.md};
`);

const SelectedBlockInfo = styled(Row)(({ theme }) => css`
  margin-bottom: ${theme.spacings.md};
`);

const HelpPopover = styled(Popover)(() => css`
  min-width: 700px;
`);

const OptionTitle = styled.p(({ theme }) => css`
  margin-bottom: ${theme.spacings.xxs};
`);

const OptionDescription = styled.p<{ $isSelected: boolean }>(({ theme, $isSelected }) => css`
  color: ${$isSelected ? theme.colors.gray[90] : theme.colors.gray[50]};
  margin-bottom: ${theme.spacings.xxs};
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

        if (!initialBlockValue) {
          if (param.type === RuleBuilderTypes.Boolean && !initialBlockValue) {
            newInitialValues[param.name] = false;
          } else {
            newInitialValues[param.name] = undefined;
          }
        } else {
          newInitialValues[param.name] = initialBlockValue;
        }
      },
      );
    }

    setInitialValues(newInitialValues);
  }, [selectedBlockDict, existingBlock]);

  const handleChange = (option: string, resetForm: () => void) => {
    sendTelemetry('select', {
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
    sendTelemetry('click', {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'pipeline-rule-builder',
      app_action_value: `${existingBlock ? 'update' : 'add'}-${type}-button`,
    });

    if (existingBlock) {
      onUpdate(values, selectedBlockDict?.name);
    } else {
      onAdd(values);
    }
  };

  const buildHelpPopover = (blockDict: BlockDict) => (
    <HelpPopover id="selected-block-Dict-help"
                 title="Function Syntax Help">
      <RuleHelperTable entries={[blockDict]} expanded={{ [blockDict.name]: true }} />
    </HelpPopover>
  );

  const optionRenderer = (option: Option, isSelected: boolean) => (
    <>
      <OptionTitle>{option.label}</OptionTitle>
      {option.description && (<OptionDescription $isSelected={isSelected}>{option.description}</OptionDescription>)}
    </>
  );

  return (
    <Row>
      <Col md={12}>
        <FormTitle>{existingBlock ? `Edit ${type}` : `Add ${type}`}</FormTitle>
        <Formik enableReinitialize onSubmit={onSubmit} initialValues={initialValues}>
          {({ resetForm, setFieldValue, isValid }) => (
            <Form>
              <Row>
                <Col md={12}>
                  <Select id={`existingBlock-select-${type}`}
                          name={`existingBlock-select-${type}`}
                          placeholder={`Select ${type}`}
                          options={options}
                          optionRenderer={optionRenderer}
                          clearable={false}
                          matchProp="label"
                          onChange={(option: string) => handleChange(option, resetForm)}
                          value={selectedBlockDict?.name || ''} />
                </Col>
              </Row>

              {selectedBlockDict && (
                <SelectedBlock>
                  <SelectedBlockInfo>
                    <Col md={12}>
                      <BlockTitle>
                        {existingBlock?.step_title || selectedBlockDict.rule_builder_name}
                        <OverlayTrigger trigger="click"
                                        rootClose
                                        placement="right"
                                        overlay={buildHelpPopover(selectedBlockDict)}>
                          <Button bsStyle="link">
                            <Icon name="question-circle"
                                  fixedWidth
                                  title="Function Syntax Help"
                                  data-testid="funcSyntaxHelpIcon" />
                          </Button>
                        </OverlayTrigger>
                      </BlockTitle>
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
                                          resetField={(fieldName) => resetField(fieldName, setFieldValue)} />
                    </Row>
                  ),
                  )}

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
            </Form>
          )}
        </Formik>
        <Errors objectWithErrors={existingBlock} />
      </Col>
    </Row>
  );
};

RuleBlockForm.propTypes = {
  existingBlock: ruleBlockPropType,
  onAdd: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onUpdate: PropTypes.func.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string,
      value: PropTypes.any,
    }),
  ).isRequired,
  order: PropTypes.number.isRequired,
  outputVariableList: outputVariablesPropType,
  selectedBlockDict: blockDictPropType,
  type: PropTypes.oneOf(['action', 'condition']).isRequired,
};

RuleBlockForm.defaultProps = {
  existingBlock: undefined,
  outputVariableList: undefined,
  selectedBlockDict: undefined,
};

export default RuleBlockForm;
