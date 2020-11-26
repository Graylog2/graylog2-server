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
import * as Immutable from 'immutable';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Select from 'components/common/Select';
import { ColorPickerPopover } from 'components/common';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import ColorPreview from 'views/components/sidebar/highlighting/ColorPreview';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

const StyledHeader = styled.h4`
  margin-bottom: 8px;
`;

const SubmitButton = styled(Button)`
  margin-right: 3px;
`;

type Props = {
  onClose: () => void,
};

const numberConditionOptions = ['==', '!=', '<=', '>=', '<', '>'].map((cond) => ({ value: cond, label: cond }));
const otherConditionOptions = ['==', '!='].map((cond) => ({ value: cond, label: cond }));

const HighlightForm = ({ onClose }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const fields = fieldTypes?.all
    ? fieldTypes.all
    : Immutable.List<FieldTypeMapping>();
  const fieldOptions = fields.map(({ name }) => ({ value: name, label: name })).toArray();

  const onSubmit = ({ field, value, color, condition }) => HighlightingRulesActions.add(
    HighlightingRule.create(field, value, condition, color),
  ).then(onClose);

  return (
    <>
      <hr />
      <StyledHeader>New Highlight rule</StyledHeader>
      <Formik onSubmit={onSubmit}
              initialValues={{
                field: undefined,
                value: undefined,
                condition: '==',
                color: '#6fecc2',
              }}>
        {() => (
          <Form className="form">
            <Field name="field">
              {({ field: { name, value, onChange } }) => (
                <Input id="field_type_controls"
                       label="Field">
                  <Select inputId="field-select"
                          onChange={(newValue) => onChange({ target: { name, value: newValue } })}
                          options={fieldOptions}
                          value={value}
                          placeholder="Pick a field" />
                </Input>
              )}
            </Field>
            <Field name="condition">
              {({ field: { name, value, onChange }, form: { values: { field: fieldValue } } }) => {
                const fieldType = fields.find(({ name: fieldName }) => fieldName === fieldValue);
                const { type } = fieldType?.type || { type: 'string' };

                return (
                  <Input id="condition-controls"
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
            <Field name="value">
              {({ field: { name, value, onChange } }) => (
                <Input id={name}
                       type="text"
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
            <SubmitButton bsStyle="success" type="submit">Create</SubmitButton>
            <Button onClick={onClose}>Cancel</Button>
          </Form>
        )}
      </Formik>
      <hr />
    </>
  );
};

export default HighlightForm;
