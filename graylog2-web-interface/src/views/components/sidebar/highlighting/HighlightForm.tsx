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

import { Input, BootstrapModalWrapper } from 'components/bootstrap';
import { Button, Modal } from 'components/graylog';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Select from 'components/common/Select';
import { ColorPickerPopover } from 'components/common';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import ColorPreview from 'views/components/sidebar/highlighting/ColorPreview';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

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

const numberConditionOptions = ['==', '!=', '<=', '>=', '<', '>'].map((cond) => ({ value: cond, label: cond }));
const otherConditionOptions = ['==', '!='].map((cond) => ({ value: cond, label: cond }));

const HighlightForm = ({ onClose, rule }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const fields = fieldTypes?.all
    ? fieldTypes.all
    : Immutable.List<FieldTypeMapping>();
  const fieldOptions = fields.map(({ name }) => ({ value: name, label: name })).toArray();

  const onSubmit = ({ field, value, color, condition }) => {
    if (rule) {
      HighlightingRulesActions.remove(rule).then(() => HighlightingRulesActions.add(
        HighlightingRule.create(field, value, condition, color),
      ).then(onClose));

      return;
    }

    HighlightingRulesActions.add(
      HighlightingRule.create(field, value, condition, color),
    ).then(onClose);
  };

  const headerTxt = rule ? 'Edit' : 'New';

  return (
    <Formik onSubmit={onSubmit}
            initialValues={{
              field: rule?.field ?? undefined,
              value: rule?.value ?? '',
              condition: rule?.condition ?? '==',
              color: rule?.color ?? '#6fecc2',
            }}>
      {() => (
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
                            value={value}
                            placeholder="Pick a field" />
                  </Input>
                )}
              </Field>
              <Field name="condition" validate={_isRequired('Condition')}>
                {({ field: { name, value, onChange }, form: { values: { field: fieldValue } }, meta }) => {
                  const fieldType = fields.find(({ name: fieldName }) => fieldName === fieldValue);
                  const { type } = fieldType?.type || { type: 'string' };

                  return (
                    <Input id="condition-controls"
                           error={meta?.error}
                           label="Condition">
                      <Select inputId="condition-select"
                              onChange={(newValue) => onChange({ target: { name, value: newValue } })}
                              options={type === 'long' ? numberConditionOptions : otherConditionOptions}
                              value={value}
                              placeholder="Choose a condition" />
                    </Input>
                  );
                }}
              </Field>
              <Field name="value" validate={_isRequired('Value')}>
                {({ field: { name, value, onChange }, meta }) => (
                  <Input id={name}
                         type="text"
                         error={meta?.error}
                         onChange={onChange}
                         value={value}
                         label="Value" />
                )}
              </Field>
              <Field name="color">
                {({ field: { name, value, onChange } }) => (
                  <Input id={name}
                         label="Color">
                    <ColorPickerPopover id="formatting-rule-color"
                                        placement="right"
                                        color={value}
                                        colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                                        triggerNode={<ColorPreview color={value} />}
                                        onChange={(newColor, _, hidePopover) => {
                                          hidePopover();
                                          onChange({ target: { name, value: newColor } });
                                        }} />
                  </Input>
                )}
              </Field>
            </Modal.Body>
            <Modal.Footer>
              <Button type="button" onClick={onClose}>Cancel</Button>
              <Button type="submit">Save</Button>
            </Modal.Footer>
          </Form>
        </BootstrapModalWrapper>
      )}

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
