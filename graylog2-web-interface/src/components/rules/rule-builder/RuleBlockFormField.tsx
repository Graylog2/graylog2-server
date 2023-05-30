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

import { FormikFormGroup, IconButton } from 'components/common';
import { ControlLabel } from 'components/bootstrap';

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
  const [showPrimaryInput, setShowPrimaryInput] = useState<boolean>(false);
  const [field] = useField(param.name);

  useEffect(() => {
    setShowPrimaryInput(false);
  }, [functionName]);

  const onPrimaryInputCancel = () => {
    setShowPrimaryInput(false);
    resetField(param.name);
  };

  const shouldHandlePrimaryParam = () => {
    if (!param.primary) return false;
    if ((order === 0)) return false;
    if (!previousOutputPresent) return false;

    return true;
  };

  const isValueSet = paramValueExists(field.value);

  const validateTextField = (value: string) : string | undefined => {
    if (paramValueExists(value) && paramValueIsVariable(value)) {
      return 'Fields starting with \'$\' are not allowed.';
    }

    return null;
  };

  const buttonAfter = () => {
    if (!shouldHandlePrimaryParam()) return null;

    if (showPrimaryInput || isValueSet) {
      return (
        <IconButton name="xmark" onClick={onPrimaryInputCancel} title="Cancel" />
      );
    }

    return (
      <IconButton name="edit" onClick={() => setShowPrimaryInput(true)} title="Edit" />
    );
  };

  const placeholder = shouldHandlePrimaryParam() && !showPrimaryInput ? 'Set output of the previous step' : '';

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
                         disabled={shouldHandlePrimaryParam() && !showPrimaryInput && !isValueSet}
                         placeholder={placeholder}
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
                         disabled={shouldHandlePrimaryParam() && !showPrimaryInput && !isValueSet}
                         placeholder={placeholder}
                         buttonAfter={buttonAfter()}
                         help={param.description}
                         {...field} />

      );
    case RuleBuilderTypes.Boolean:
      return (
        <>
          {(shouldHandlePrimaryParam() && !showPrimaryInput && !isValueSet) ? (placeholder) : (
            <>
              <ControlLabel className="col-sm-3">{param.name}</ControlLabel>
              <FormikFormGroup type="checkbox"
                               key={`${functionName}_${param.name}`}
                               name={param.name}
                               label={field.value ? 'true' : 'false'}
                               help={param.description}
                               checked={field.value}
                               {...field} />
            </>
          )}
          {buttonAfter()}
        </>

      );
    default:
      return null;
  }
};

export default RuleBlockFormField;
