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
import { Form, Formik } from 'formik';
import styled from 'styled-components';

import { FormikFormGroup, Select, FormSubmit } from 'components/common';
import { Col, Row } from 'components/bootstrap';

import { RuleBuilderSupportedTypes } from './types';
import type { BlockDict, BlockFieldDict, BlockType, RuleBlock } from './types';

type Props = {
  existingBlock: RuleBlock,
  onAdd: (values: {[key: string]: any}) => void,
  onCancel: () => void,
  onSelect: (option: string) => void,
  onUpdate: (values: {[key: string]: any}) => void
  options: Array<{ label: string, value: any }>,
  selectedBlockDict: BlockDict,
  type: BlockType,
}

const Title = styled.h3`
  margin-bottom: 5px;
`;

const SelectedBlockInfo = styled(Row)`
  margin-bottom: 20px;
  margin-top: 20px;
`;

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
                <>
                  <SelectedBlockInfo>
                    <Col md={12}>
                      <Title>{selectedBlockDict.rule_builder_title || selectedBlockDict.name}</Title>
                      <p>{selectedBlockDict.description}</p>
                    </Col>
                  </SelectedBlockInfo>

                  {/* eslint-disable-next-line react/no-array-index-key */}
                  {selectedBlockDict.params.map((param, key) => <Row key={key}>{buildParamField(param)}</Row>)}

                  <FormSubmit bsSize="small"
                              submitButtonText={`${existingBlock ? 'Update' : 'Add'}`}
                              onCancel={() => { resetForm(); onCancel(); }} />

                </>
              )}
            </Form>
          )}
        </Formik>
      </Col>
    </Row>
  );
};

export default RuleBlockForm;
