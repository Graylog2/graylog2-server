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

import { FormikFormGroup } from 'components/common';
import { Button, ControlLabel } from 'components/bootstrap';

import { RuleBuilderTypes } from './types';
import type { BlockFieldDict } from './types';
import { paramValueExists, paramValueIsVariable } from './helpers';

type Props = {
  param: BlockFieldDict,
  functionName: string,
  order: number,
  previousOutputPresent: boolean,
  resetField: (fieldName: string) => void;
}

const RuleBlockFormField = ({ param, functionName, order, previousOutputPresent, resetField }: Props) => {
  const [primaryInputToggle, setPrimaryInputToggle] = useState<'custom' | 'select' | undefined>(undefined);
  const [field, fieldMeta] = useField(param.name);

  useEffect(() => {
    setPrimaryInputToggle(undefined);
  }, [functionName]);

  const shouldHandlePrimaryParam = () => {
    if (!param.primary) return false;
    if ((order === 0)) return false;
    if (!previousOutputPresent) return false;

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

  const buttonAfter = () => {
    if (!shouldHandlePrimaryParam()) return null;

    return (<Button onClick={() => onPrimaryInputToggle('select')}>Choose output to use</Button>);
  };

  const outputVariableOptions : Array<{ label: string, value: any}> = [
    { label: 'Output from step 1', value: '$output1' },
    { label: 'Output from step 2', value: '$output2' },
    { label: 'Output from step 3', value: '$output3' },
  ];

  const showOutputVariableSelect = () => {
    if (!shouldHandlePrimaryParam()) return false;

    if (primaryInputToggle === 'select') return true;

    if (typeof primaryInputToggle !== 'undefined') return false;

    return !fieldMeta.initialValue || paramValueIsVariable(fieldMeta.initialValue);
  };

  if (showOutputVariableSelect()) {
    return (
      <FormikFormGroup type="select"
                       key={`${functionName}_${param.name}`}
                       name={param.name}
                       label={param.name}
                       required={!param.optional}
                       buttonAfter={<Button onClick={() => onPrimaryInputToggle('custom')}>{`Set custom ${param.name}`}</Button>}
                       help={param.description}
                       {...field}>
        {outputVariableOptions.map(({ label, value }) => <option key={`option-${value}`} value={value}>{label}</option>)}
      </FormikFormGroup>
    );
  }

  switch (param.type) {
    case RuleBuilderTypes.String:
    case RuleBuilderTypes.Object:
      return (
        <FormikFormGroup type="text"
                         key={`${functionName}_${param.name}`}
                         name={param.name}
                         label={param.name}
                         required={!param.optional}
                         validate={validateTextField}
                         buttonAfter={buttonAfter()}
                         help={param.description}
                         {...field} />
      );
    case RuleBuilderTypes.Number:
      return (
        <FormikFormGroup type="number"
                         key={`${functionName}_${param.name}`}
                         name={param.name}
                         label={param.name}
                         required={!param.optional}
                         buttonAfter={buttonAfter()}
                         help={param.description}
                         {...field} />

      );
    case RuleBuilderTypes.Boolean:
      return (
        <>
          <ControlLabel className="col-sm-3">{param.name}</ControlLabel>
          <FormikFormGroup type="checkbox"
                           key={`${functionName}_${param.name}`}
                           name={param.name}
                           label={field.value ? 'true' : 'false'}
                           help={param.description}
                           checked={field.value}
                           buttonAfter={buttonAfter()}
                           {...field} />
        </>
      );
    default:
      return null;
  }
};

export default RuleBlockFormField;
