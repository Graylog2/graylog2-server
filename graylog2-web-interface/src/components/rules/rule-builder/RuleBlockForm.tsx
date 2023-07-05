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
import { Formik } from 'formik';
import styled, { css } from 'styled-components';

import { FormSubmit, Select } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import RuleBlockFormField from 'components/rules/rule-builder/RuleBlockFormField';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

import Errors from './Errors';
import { paramValueIsVariable } from './helpers';
import { ruleBlockPropType, blockDictPropType, RuleBuilderTypes } from './types';
import type { BlockType, RuleBlock, BlockDict, BlockFieldDict } from './types';

type Props = {
  existingBlock?: RuleBlock,
  onAdd: (values: { [key: string]: any }) => void,
  onCancel: () => void,
  onSelect: (option: string) => void,
  onUpdate: (values: { [key: string]: any }, functionName: string) => void,
  previousOutputPresent: boolean,
  options: Array<{ label: string, value: any }>,
  order: number,
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

const RuleBlockForm = ({
  existingBlock,
  onAdd,
  onCancel,
  onSelect,
  onUpdate,
  options,
  order,
  previousOutputPresent,
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
        } else if (paramValueIsVariable(initialBlockValue)) {
          newInitialValues[param.name] = undefined;
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

  const handleSubmit = (values: { [key: string]: any }) => {
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

  return (
    <Row>
      <Col md={12}>
        <FormTitle>{existingBlock ? `Edit ${type}` : `Add ${type}`}</FormTitle>
        <Formik enableReinitialize onSubmit={handleSubmit} initialValues={initialValues}>
          {({ resetForm, setFieldValue, values }) => (
            <>
              <Row>
                <Col md={12}>
                  <Select id={`existingBlock-select-${type}`}
                          name={`existingBlock-select-${type}`}
                          placeholder={`Select ${type}`}
                          options={options}
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
                        {existingBlock?.step_title || selectedBlockDict.name}
                      </BlockTitle>
                      <BlockDescription>{selectedBlockDict.description}</BlockDescription>
                    </Col>
                  </SelectedBlockInfo>

                  {selectedBlockDict.params.map((param) => (
                    <Row key={`${order}_${param.name}`}>
                      <RuleBlockFormField param={param}
                                          functionName={selectedBlockDict.name}
                                          order={order}
                                          previousOutputPresent={previousOutputPresent}
                                          resetField={(fieldName) => resetField(fieldName, setFieldValue)} />
                    </Row>
                  ),
                  )}

                  <FormSubmit bsSize="small"
                              submitButtonText={`${existingBlock ? 'Update' : 'Add'}`}
                              submitButtonType="button"
                              onSubmit={() => handleSubmit(values)}
                              onCancel={() => {
                                resetForm();
                                onCancel();
                              }} />

                </SelectedBlock>
              )}
            </>
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
  previousOutputPresent: PropTypes.bool.isRequired,
  selectedBlockDict: blockDictPropType,
  type: PropTypes.oneOf(['action', 'condition']).isRequired,
};

RuleBlockForm.defaultProps = {
  existingBlock: undefined,
  selectedBlockDict: undefined,
};

export default RuleBlockForm;
