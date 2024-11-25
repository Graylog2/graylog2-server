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
import { useCallback, useContext, useMemo } from 'react';
import * as Immutable from 'immutable';
import { Formik, Form, Field } from 'formik';
import isNil from 'lodash/isNil';

import { defaultCompare } from 'logic/DefaultCompare';
import { Input, BootstrapModalWrapper, Modal } from 'components/bootstrap';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Select from 'components/common/Select';
import HighlightingRule, {
  ConditionLabelMap,
  StringConditionLabelMap,
} from 'views/logic/views/formatting/highlighting/HighlightingRule';
import HighlightingColorForm, { createNewColor } from 'views/components/sidebar/highlighting/HighlightingColorForm';
import type { StaticColorObject, GradientColorObject } from 'views/components/sidebar/highlighting/HighlightingColorForm';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import inferTypeForSeries from 'views/logic/fieldtypes/InferTypeForSeries';
import type HighlightingColor from 'views/logic/views/formatting/highlighting/HighlightingColor';
import {
  GradientColor,
  StaticColor,
} from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { ModalSubmit } from 'components/common';
import useAppDispatch from 'stores/useAppDispatch';
import { addHighlightingRule, updateHighlightingRule } from 'views/logic/slices/highlightActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

type Props = {
  onClose: () => void,
  rule?: HighlightingRule | null | undefined,
};

const _isRequired = (field) => (value: string) => {
  if (['', null, undefined].includes(value)) {
    return `${field} is required`;
  }

  return undefined;
};

const numberConditionOptions = Object.entries(ConditionLabelMap).map(([value, label]) => ({ value, label }));
const otherConditionOptions = Object.entries(StringConditionLabelMap).map(([value, label]) => ({ value, label }));

const fieldTypeFor = (fields: FieldTypeMappingsList, selectedField: string) => (isFunction(selectedField)
  ? inferTypeForSeries(Series.forFunction(selectedField), fields)
  : fields.find((field) => field.name === selectedField));

const colorToObject = (color: HighlightingColor | undefined): StaticColorObject | GradientColorObject => {
  const defaultColorType = 'static';

  if (color?.type === 'static') {
    const { type, color: staticColor } = color as StaticColor;

    return {
      type,
      color: staticColor,
    };
  }

  if (color?.type === 'gradient') {
    const { type, gradient, upper, lower } = color as GradientColor;

    return {
      type,
      gradient,
      upper,
      lower,
    };
  }

  return createNewColor(defaultColorType);
};

const colorFromObject = (color: StaticColorObject | GradientColorObject) => {
  if (color?.type === 'static') {
    return StaticColor.create(color.color);
  }

  if (color?.type === 'gradient') {
    const { gradient, lower, upper } = color;

    return GradientColor.create(gradient, lower, upper);
  }

  return undefined;
};

const HighlightForm = ({ onClose, rule }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();
  const fields = fieldTypes?.all
    ? fieldTypes.all
    : Immutable.List<FieldTypeMapping>();
  const fieldOptions = useMemo(() => fields.map(({ name }) => ({ value: name, label: name }))
    .sort((optA, optB) => defaultCompare(optA.label, optB.label))
    .toArray(), [fields]);
  const dispatch = useAppDispatch();

  const onSubmit = useCallback(({ field, value, color, condition }) => {
    const newColor = colorFromObject(color);

    sendTelemetry(TELEMETRY_EVENT_TYPE[`SEARCH_SIDEBAR_HIGHLIGHT_${rule ? 'UPDATED' : 'CREATED'}`], {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'search-sidebar-highlight',
    });

    if (rule) {
      return dispatch(updateHighlightingRule(rule, { field, value, condition, color: newColor })).then(onClose);
    }

    return dispatch(addHighlightingRule(HighlightingRule.create(field, value, condition, newColor))).then(onClose);
  }, [dispatch, location.pathname, onClose, rule, sendTelemetry]);

  const headerPrefix = rule ? 'Edit' : 'Create';
  const submitButtonPrefix = rule ? 'Update' : 'Create';

  return (
    <Formik onSubmit={onSubmit}
            validateOnMount
            initialValues={{
              field: rule?.field,
              value: isNil(rule?.value) ? '' : String(rule?.value),
              condition: rule?.condition ?? 'equal',
              color: colorToObject(rule?.color),
            }}>
      {({ isValid, values: { field: selectedField } }) => {
        const selectedFieldType = fieldTypeFor(fields, selectedField);
        const isNumeric = selectedFieldType?.type?.isNumeric() ?? false;

        return (
          <BootstrapModalWrapper showModal
                                 onHide={onClose}>
            <Form className="form"
                  data-testid={`${headerPrefix}-highlighting-rule-dialog`}>
              <Modal.Header>
                <Modal.Title>{headerPrefix} Highlighting Rule</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Field name="field" validate={_isRequired('Field')}>
                  {({ field: { name, value, onChange }, meta }) => (
                    <Input id="field_type_controls"
                           error={meta?.error}
                           label="Field">
                      <Select inputId="field-select"
                              onChange={(newValue) => onChange({ target: { name, value: newValue } })}
                              options={fieldOptions}
                              allowCreate
                              value={value}
                              placeholder="Pick a field" />
                    </Input>
                  )}
                </Field>
                <Field name="condition" validate={_isRequired('Condition')}>
                  {({ field: { name, value, onChange }, meta }) => (
                    <Input id="condition-controls"
                           error={meta?.error}
                           label="Condition">
                      <Select inputId="condition-select"
                              onChange={(newValue) => onChange({ target: { name, value: newValue } })}
                              options={isNumeric ? numberConditionOptions : otherConditionOptions}
                              value={value}
                              placeholder="Choose a condition" />
                    </Input>
                  )}
                </Field>
                <Field name="value" validate={_isRequired('Value')}>
                  {({ field: { name, value, onChange }, meta }) => (
                    <Input id={name}
                           type="text"
                           error={meta?.error}
                           onChange={onChange}
                           value={value ?? ''}
                           label="Value" />
                  )}
                </Field>
                <HighlightingColorForm field={selectedFieldType} />
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit onCancel={onClose}
                             disabledSubmit={!isValid}
                             submitButtonText={`${submitButtonPrefix} rule`} />
              </Modal.Footer>
            </Form>
          </BootstrapModalWrapper>
        );
      }}

    </Formik>
  );
};

export default HighlightForm;
