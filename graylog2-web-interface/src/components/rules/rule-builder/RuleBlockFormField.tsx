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
import { useField } from 'formik';

import { FormikFormGroup, InputOptionalInfo } from 'components/common';
import { Button, ControlLabel } from 'components/bootstrap';

import { RuleBuilderTypes } from './types';
import type { OutputVariables, BlockFieldDict, BlockType } from './types';

type Props = {
  param: BlockFieldDict,
  functionName: string,
  blockId?: string
  order: number,
  outputVariableList?: OutputVariables,
  blockType: BlockType,
  resetField: (fieldName: string) => void;
}

const SupportedFieldTypes = [RuleBuilderTypes.String, RuleBuilderTypes.Object, RuleBuilderTypes.Number, RuleBuilderTypes.Boolean];

const RuleBlockFormField = ({ param, functionName, blockId, order, outputVariableList = [], blockType, resetField }: Props) => {
  const [primaryInputToggle, setPrimaryInputToggle] = useState<'custom' | 'select' | undefined>(undefined);
  const [field, fieldMeta] = useField(param.name);

  useEffect(() => {
    setPrimaryInputToggle(undefined);
  }, [functionName]);

  const paramValueExists = (paramValue: string | number | boolean | undefined) : boolean => (
    typeof paramValue !== 'undefined' && paramValue !== null);

  const paramValueIsVariable = (paramValue: string | number | boolean | undefined) : boolean => (
    typeof paramValue === 'string' && paramValue.startsWith('$'));

  const shouldHandlePrimaryParam = () => {
    if (!param.rule_builder_variable) return false;
    if ((order === 0)) return false;

    return true;
  };

  const validateTextField = (value: string) : string | undefined => {
    if (paramValueExists(value) && paramValueIsVariable(value)) {
      return 'Fields starting with \'$\' are not allowed.';
    }

    return null;
  };

  const onPrimaryInputToggle = (type: 'custom' | 'select') => {
    setPrimaryInputToggle(type);
    resetField(param.name);
  };

  const filteredOutputVariableList = () => (
    outputVariableList.filter((outputVariable) => {
      if (outputVariable.blockId === blockId) return false;

      if (outputVariable.stepOrder >= order) return false;

      if (param.type === RuleBuilderTypes.Object) return true;

      return (outputVariable.variableType === param.type);
    }));

  const primaryInputButtonAfter = () => {
    if (!shouldHandlePrimaryParam() || filteredOutputVariableList().length <= 0) return null;

    return (<Button onClick={() => onPrimaryInputToggle('select')}>Use output from previous steps</Button>);
  };

  const showOutputVariableSelect = () => {
    if (!shouldHandlePrimaryParam()) return false;

    if (filteredOutputVariableList().length <= 0) return false;

    if (!SupportedFieldTypes.includes(param.type)) return false;

    if (primaryInputToggle === 'select') return true;

    if (typeof primaryInputToggle !== 'undefined') return false;

    return !fieldMeta.initialValue || fieldMeta.initialValue === '' || paramValueIsVariable(fieldMeta.initialValue);
  };

  const labelText = (labelParam: BlockFieldDict): React.ReactElement | string => {
    if (labelParam.optional) {
      return <>{labelParam.name} <InputOptionalInfo /></>;
    }

    return labelParam.name;
  };

  if (showOutputVariableSelect()) {
    return (
      <FormikFormGroup type="select"
                       key={`${functionName}_${param.name}`}
                       name={param.name}
                       label={labelText(param)}
                       required={!param.optional}
                       buttonAfter={<Button onClick={() => onPrimaryInputToggle('custom')}>{`Set custom ${param.name}`}</Button>}
                       help={param.description}
                       {...field}>
        <>
          <option key="placeholder" value="">Select output from list</option>
          {filteredOutputVariableList().map(({ variableName, stepOrder }) => (
            <option key={`option-${variableName}`} value={variableName}>{`Output from step ${(stepOrder + 1)} (${variableName})`}</option>),
          )}
        </>
      </FormikFormGroup>
    );
  }

  const typeNotFoundErrorMessage = `No previous action returns type ${param.type.slice(param.type.lastIndexOf('.') + 1)}`;

  switch (param.type) {
    case RuleBuilderTypes.String:
    case RuleBuilderTypes.Object:
      return (
        <FormikFormGroup type="text"
                         key={`${functionName}_${param.name}`}
                         name={param.name}
                         label={labelText(param)}
                         required={!param.optional}
                         validate={validateTextField}
                         buttonAfter={primaryInputButtonAfter()}
                         help={param.description}
                         {...field} />
      );
    case RuleBuilderTypes.Number:
      return (
        <FormikFormGroup type="number"
                         key={`${functionName}_${param.name}`}
                         name={param.name}
                         label={labelText(param)}
                         required={!param.optional}
                         buttonAfter={primaryInputButtonAfter()}
                         help={param.description}
                         {...field} />

      );
    case RuleBuilderTypes.Boolean:
      return (
        <>
          <ControlLabel className="col-sm-3">{labelText(param)}</ControlLabel>
          <FormikFormGroup type="checkbox"
                           key={`${functionName}_${param.name}`}
                           name={param.name}
                           label={field.value ? 'true' : 'false'}
                           help={param.description}
                           checked={field.value}
                           buttonAfter={primaryInputButtonAfter()}
                           {...field} />
        </>
      );
    default:
      if (blockType === 'action') {
        return (
          <FormikFormGroup type="select"
                           key={`${functionName}_${param.name}`}
                           name={param.name}
                           label={labelText(param)}
                           required={!param.optional}
                           help={
                            (!filteredOutputVariableList().length && param.optional)
                              ? typeNotFoundErrorMessage
                              : param.description
                           }
                           error={
                             (!filteredOutputVariableList().length && !param.optional)
                               ? typeNotFoundErrorMessage
                               : undefined
                           }
                           {...field}>
            <>
              <option key="placeholder" value="">Select output from list</option>
              {filteredOutputVariableList().map(({ variableName, stepOrder }) => (
                <option key={`option-${variableName}`} value={variableName}>{`Output from step ${(stepOrder + 1)} (${variableName})`}</option>),
              )}
            </>
          </FormikFormGroup>
        );
      }

      return null;
  }
};

export default RuleBlockFormField;
