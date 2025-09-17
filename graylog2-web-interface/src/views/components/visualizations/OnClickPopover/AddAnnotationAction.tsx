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
import React, { useState } from 'react';
import { Formik, Form, Field } from 'formik';
import styled, { css } from 'styled-components';
import random from 'lodash/random';

import Popover from 'components/common/Popover';
import { Button, Col } from 'components/bootstrap';
import { ModalSubmit, FormikInput } from 'components/common';
import { colors as defaultColors, colors } from 'views/components/visualizations/Colors';
import ColorPicker from 'components/common/ColorPicker';

import Annotation = module;

const ColorHintWrapper = styled.div`
  width: 25px;
  height: 25px;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const ColorHint = styled.div(
  ({ color, theme }) => css`
    cursor: pointer;
    background-color: ${color};
    width: ${theme.spacings.md};
    height: ${theme.spacings.md};
  `,
);

const getInitialValues = () => {
  const palette = colors[random(0, colors.length - 1)];
  const randomColor = palette[random(0, palette.length - 1)];

  return {
    color: randomColor,
    note: '',
  };
};

export type AddAnnotationFormValues = { note: string; color: string; showReferenceLines: boolean };

type Props = {
  onAddAnnotation: (newAnnotation: AddAnnotationFormValues) => void;
};

const AddAnnotationAction = ({ onAddAnnotation }: Props) => {
  const [showPopover, setShowPopover] = useState(false);
  const [showColorPopover, setShowColorPopover] = useState(false);
  const toggleColorPopoverPopover = () => setShowColorPopover((cur) => !cur);
  const togglePopoverPopover = () => setShowPopover((cur) => !cur);

  const initialValues = getInitialValues();

  const onSubmit = (values: { note: string; color: string; showReferenceLines }) => {
    onAddAnnotation(values);
    togglePopoverPopover();
  };

  return (
    <Popover opened={showPopover} withinPortal={false}>
      <Popover.Target>
        <Button bsSize="xs" onClick={togglePopoverPopover}>
          Add annotation
        </Button>
      </Popover.Target>
      <Popover.Dropdown>
        <Formik initialValues={initialValues} onSubmit={onSubmit}>
          {({ isValid, isSubmitting, setFieldValue, values }) => (
            <Form>
              <div>
                <Col sm={11}>
                  <FormikInput id="note" name="note" label="Note" type="textarea" />
                </Col>
                <Col sm={1}>
                  <Popover position="top" withArrow opened={showColorPopover} withinPortal={false}>
                    <Popover.Target>
                      <ColorHintWrapper>
                        <ColorHint aria-label="Color Hint" onClick={toggleColorPopoverPopover} color={values.color} />
                      </ColorHintWrapper>
                    </Popover.Target>
                    <Popover.Dropdown title="Color configuration for threshold">
                      <ColorPicker
                        color={values.color}
                        colors={defaultColors}
                        onChange={(color) => {
                          toggleColorPopoverPopover();

                          return setFieldValue('color', color);
                        }}
                      />
                    </Popover.Dropdown>
                  </Popover>
                </Col>
                <Col sm={11}>
                  <Field name="showReferenceLines">
                    {({ field: { name, value = false } }) => (
                      <FormikInput
                        type="checkbox"
                        wrapperClassName="col-sm-12"
                        label="Show reference lines"
                        id={`${name}-input`}
                        name={name}
                        onChange={() => {
                          const newVal = !value;
                          setFieldValue(name, newVal);
                        }}
                      />
                    )}
                  </Field>
                </Col>

                <Button bsSize="xs" bsStyle="success" disabled={isSubmitting && !isValid} type="submit">
                  Add annotation
                </Button>
              </div>
            </Form>
          )}
        </Formik>
      </Popover.Dropdown>
    </Popover>
  );
};

export default AddAnnotationAction;
