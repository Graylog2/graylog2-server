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
import { useContext } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import { Formik, Form, Field } from 'formik';

import { defaultCompare } from 'views/logic/DefaultCompare';
import { Input, BootstrapModalWrapper } from 'components/bootstrap';
import { Button, Modal } from 'components/graylog';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Select from 'components/common/Select';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import HighlightingRule, {
  ConditionLabelMap,
  StringConditionLabelMap,
} from 'views/logic/views/formatting/highlighting/HighlightingRule';
import HighlightingColorForm, { createNewColor } from 'views/components/sidebar/highlighting/HighlightingColorForm';
import type { StaticColorObject, GradientColorObject } from 'views/components/sidebar/highlighting/HighlightingColorForm';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import Series, { isFunction } from 'views/logic/aggregationbuilder/Series';
import inferTypeForSeries from 'views/logic/fieldtypes/InferTypeForSeries';
import HighlightingColor, {
  GradientColor,
  StaticColor,
} from 'views/logic/views/formatting/highlighting/HighlightingColor';

type Props = {
  onClose: () => void,
  rule: HighlightingRule | null | undefined,
};

const _isRequired = (field) => (value) => {
  if (!value || value === '') {
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
  const fields = fieldTypes?.all
    ? fieldTypes.all
    : Immutable.List<FieldTypeMapping>();
  const fieldOptions = fields.map(({ name }) => ({ value: name, label: name }))
    .sort((optA, optB) => defaultCompare(optA.label, optB.label))
    .toArray();

  const onSubmit = ({ field, value, color, condition }) => {
    const newColor = colorFromObject(color);

    if (rule) {
      HighlightingRulesActions.update(rule, { field, value, condition, color: newColor }).then(onClose);

      return;
    }

    HighlightingRulesActions.add(HighlightingRule.create(field, value, condition, newColor)).then(onClose);
  };

  const headerTxt = rule ? 'Edit' : 'New';

  return (
    <Formik onSubmit={onSubmit}
            validateOnMount
            initialValues={{
              field: rule?.field,
              value: rule?.value,
              condition: rule?.condition ?? 'equal',
              color: colorToObject(rule?.color),
            }}>
      {({ isValid, values: { field: selectedField } }) => {
        const selectedFieldType = fieldTypeFor(fields, selectedField);
        const isNumeric = selectedFieldType?.type?.isNumeric() ?? false;

        return (
          <BootstrapModalWrapper showModal
                                 onClose={onClose}>
            <Form className="form">
              <Modal.Header>
                <Modal.Title>{headerTxt} Highlighting Rule</Modal.Title>
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
                <Button type="button" onClick={onClose}>Cancel</Button>
                <Button type="submit" disabled={!isValid} bsStyle="primary">Save</Button>
              </Modal.Footer>
            </Form>
          </BootstrapModalWrapper>
        );
      }}

    </Formik>
  );
};

HighlightForm.propTypes = {
  onClose: PropTypes.func.isRequired,
  rule: PropTypes.object,
};

HighlightForm.defaultProps = {
  rule: undefined,
};

export default HighlightForm;
