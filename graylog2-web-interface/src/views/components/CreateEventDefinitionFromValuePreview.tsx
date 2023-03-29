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
import React, { useContext, useEffect, useMemo, useRef, useState, useReducer } from 'react';
import styled, { css } from 'styled-components';

import { Checkbox, Modal, Button } from 'components/bootstrap';
import { ActionContext } from 'views/logic/ActionContext';

const CheckboxWithIndeterminate = ({ label, value, onChange }) => {
  const checkboxRef = React.useRef();

  React.useEffect(() => {
    if (value === CHECKBOX_STATES.Checked) {
      checkboxRef.current.checked = true;
      checkboxRef.current.indeterminate = false;
    } else if (value === CHECKBOX_STATES.Empty) {
      checkboxRef.current.checked = false;
      checkboxRef.current.indeterminate = false;
    } else if (value === CHECKBOX_STATES.Indeterminate) {
      checkboxRef.current.checked = false;
      checkboxRef.current.indeterminate = true;
    }
  }, [value]);

  return (
    <Checkbox label={label} inputRef={(ref) => { checkboxRef.current = ref; }} onChange={onChange} />
  );
};

type Item<T> = { key: string, value: T, checked: boolean}
type Strategy = 'ALL' | 'ROW' | 'COL' | 'EXACT' |'CUSTOM';
type State = {
  strategy: Strategy,
  aggregation?: Array<Item<string>>,
  query?: Array<Item<string>>
  streams?: Item<Array<string>>,
  search_within_ms?: Item<number>
}

const reducer = (state: State, action) => {
  switch (action.type) {
    case 'COMPLETE':
    default:
      return state;
  }
};

const CHECKBOX_STATES = {
  Checked: 'Checked',
  Empty: 'Empty',
  Indeterminate: 'Indeterminate',
};

const CheckBoxGroup = ({ data, checked, name, onChange }) => {
  return (
    <div>
      <CheckboxWithIndeterminate onChange={onChange} />
      <div>
        <Checkbox />
      </div>

    </div>
  );
};

const CreateEventDefinitionFromValuePreview = () => {
  const [show, setShow] = useState(true);
  const [todos, dispatch] = useReducer(reducer, {});
  const [checked, setChecked] = React.useState(CHECKBOX_STATES.Empty);

  const handleChange = () => {
    let updatedChecked;

    if (checked === CHECKBOX_STATES.Checked) {
      updatedChecked = CHECKBOX_STATES.Empty;
    } else if (checked === CHECKBOX_STATES.Empty) {
      updatedChecked = CHECKBOX_STATES.Checked;
    } else if (checked === CHECKBOX_STATES.Indeterminate) {
      updatedChecked = CHECKBOX_STATES.Checked;
    }

    setChecked(updatedChecked);
  };

  const { checkboxStatus, checkboxRef } = useCheckboxStatus([{ id: '1' }, { id: '2' }], ['1']);

  return (
    <Modal onHide={() => {}} show={show}>
      <Modal.Header closeButton>
        <Modal.Title>Modal Title</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <CheckboxWithIndeterminate label="Label" value={checked} onChange={handleChange} />
        <Checkbox inputRef={(ref) => { checkboxRef.current = ref; }} checked={checkboxStatus === 'CHECKED'} />
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => {}}>
          Cancel
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateEventDefinitionFromValuePreview;
