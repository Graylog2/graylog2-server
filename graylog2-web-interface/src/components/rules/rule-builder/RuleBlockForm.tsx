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

import { Select, FormikInput } from 'components/common';
import { Button } from 'components/bootstrap';
import { RuleBuilderSupportedTypes } from 'hooks/useRuleBuilder';
import type { BlockDict, BlockFieldDict, RuleBlock } from 'hooks/useRuleBuilder';
import type { BlockType } from 'src/components/rules/rule-builder/RuleBuilderBlock';

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
          <FormikInput type="text"
                       key={`${selectedBlockDict.name}_${param.name}`}
                       name={param.name}
                       id={param.name}
                       label={param.name}
                       required={!param.optional}
                       help={param.description} />
        );
      case RuleBuilderSupportedTypes.Number:
        return (
          <FormikInput type="number"
                       key={`${selectedBlockDict.name}_${param.name}`}
                       name={param.name}
                       id={param.name}
                       label={param.name}
                       required={!param.optional}
                       help={param.description} />
        );
      case RuleBuilderSupportedTypes.Boolean:
        return (
          <FormikInput type="checkbox"
                       key={`${selectedBlockDict.name}_${param.name}`}
                       name={param.name}
                       id={param.name}
                       label={param.name}
                       required={!param.optional}
                       help={param.description} />
        );
      case RuleBuilderSupportedTypes.Object:
        return (
          <FormikInput type="textarea"
                       id={param.name}
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
    <>
      <h3>{selectedBlockDict?.rule_builder_title || selectedBlockDict?.name}</h3>
      <p>{selectedBlockDict?.description}</p>
      <Formik onSubmit={existingBlock ? onUpdate : onAdd} initialValues={buildInitialValues()}>
        {({ resetForm }) => (
          <Form>
            <Select id="existingBlock-select"
                    name="existingBlock-select"
                    placeholder={`Select a ${type}`}
                    options={options}
                    clearable={false}
                    matchProp="label"
                    onChange={(option: string) => { resetForm(); onSelect(option); }}
                    value={selectedBlockDict?.name || ''} />
            {selectedBlockDict && (
            <>
              {selectedBlockDict.params.map((param) => buildParamField(param))}
              <Button type="submit">{existingBlock ? 'Update' : 'Add'}</Button>
              {(existingBlock || selectedBlockDict) && <Button onClick={() => { resetForm(); onCancel(); }}>Cancel</Button>}
            </>
            )}
          </Form>
        )}
      </Formik>
    </>
  );
};

export default RuleBlockForm;
