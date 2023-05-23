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
import React, { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Form, Formik } from 'formik';
import styled, { css } from 'styled-components';

import { FormikFormGroup, Select, FormSubmit } from 'components/common';
import { Col, Row } from 'components/bootstrap';

import { RuleBuilderSupportedTypes, ruleBlockPropType, blockDictPropType } from './types';
import type { BlockFieldDict, BlockType, RuleBlock, BlockDict } from './types';
import { replaceVariablesWithParams } from './helpers';

type Props = {
  existingBlock?: RuleBlock,
  onAdd: (values: {[key: string]: any}) => void,
  onCancel: () => void,
  onSelect: (option: string) => void,
  onUpdate: (values: {[key: string]: any}) => void
  options: Array<{ label: string, value: any }>,
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
  selectedBlockDict,
  type,
}: Props) => {
  const buildParamField = (param: BlockFieldDict) => {
    switch (param.type) {
      case RuleBuilderSupportedTypes.String:
      case RuleBuilderSupportedTypes.Message:
        return (
          <FormikFormGroup type="text"
                           key={`${selectedBlockDict.name}_${param.name}`}
                           name={param.name}
                           label={param.name}
                           required={!param.optional}
                           help={param.description} />
        );
      case RuleBuilderSupportedTypes.Number:
        return (
          <FormikFormGroup type="number"
                           key={`${selectedBlockDict.name}_${param.name}`}
                           name={param.name}
                           label={param.name}
                           required={!param.optional}
                           help={param.description} />
        );
      case RuleBuilderSupportedTypes.Boolean:
        return (
          <FormikFormGroup type="checkbox"
                           key={`${selectedBlockDict.name}_${param.name}`}
                           name={param.name}
                           label={param.name}
                           required={!param.optional}
                           help={param.description} />
        );
      case RuleBuilderSupportedTypes.Object:
        return (
          <FormikFormGroup type="textarea"
                           name={param.name}
                           key={`${selectedBlockDict.name}_${param.name}`}
                           rows={4}
                           label={param.name}
                           required={!param.optional}
                           help={param.description} />
        );
      default:
        return null;
    }
  };

  const buildInitialValues = useCallback(() => {
    const initialValues = {};

    if (!selectedBlockDict) { return initialValues; }

    selectedBlockDict.params.forEach((param) => {
      const initialBlockValue = existingBlock?.function === selectedBlockDict.name ? existingBlock?.params[param.name] : undefined;

      initialValues[param.name] = initialBlockValue;
    },
    );

    return initialValues;
  }, [selectedBlockDict, existingBlock]);

  return (
    <Row>
      <Col md={12}>
        <FormTitle>{existingBlock ? `Edit ${type}` : `Add ${type}`}</FormTitle>
        <Formik onSubmit={existingBlock ? onUpdate : onAdd} initialValues={buildInitialValues()}>
          {({ resetForm }) => (
            <Form>
              <Row>
                <Col md={12}>
                  <Select id="existingBlock-select"
                          name="existingBlock-select"
                          placeholder={`Select ${type}`}
                          options={options}
                          clearable={false}
                          matchProp="label"
                          onChange={(option: string) => { resetForm(); onSelect(option); }}
                          value={selectedBlockDict?.name || ''} />
                </Col>
              </Row>

              {selectedBlockDict && (
                <SelectedBlock>
                  <SelectedBlockInfo>
                    <Col md={12}>
                      <BlockTitle>
                        {replaceVariablesWithParams(existingBlock?.params, selectedBlockDict.rule_builder_title || selectedBlockDict.name)}
                      </BlockTitle>
                      <BlockDescription>{selectedBlockDict.description}</BlockDescription>
                    </Col>
                  </SelectedBlockInfo>

                  {/* eslint-disable-next-line react/no-array-index-key */}
                  {selectedBlockDict.params.map((param, key) => <Row key={key}>{buildParamField(param)}</Row>)}

                  <FormSubmit bsSize="small"
                              submitButtonText={`${existingBlock ? 'Update' : 'Add'}`}
                              onCancel={() => { resetForm(); onCancel(); }} />

                </SelectedBlock>
              )}
            </Form>
          )}
        </Formik>
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
  selectedBlockDict: blockDictPropType,
  type: PropTypes.oneOf(['action', 'condition']).isRequired,
};

RuleBlockForm.defaultProps = {
  existingBlock: undefined,
  selectedBlockDict: undefined,
};

export default RuleBlockForm;
