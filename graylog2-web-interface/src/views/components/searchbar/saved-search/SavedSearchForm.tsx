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
import React from 'react';
import { Position } from 'react-overlays';
import styled from 'styled-components';

import { ButtonToolbar, Button, ControlLabel, FormControl, FormGroup, Popover } from 'components/bootstrap';
import { Portal } from 'components/common';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';

import styles from './SavedSearchForm.css';

type Props = {
  onChangeTitle: (event: React.ChangeEvent<HTMLInputElement>) => void,
  saveSearch: () => void,
  saveAsSearch: () => void,
  disableCreateNew: boolean,
  toggleModal: () => void,
  isCreateNew: boolean,
  value: string,
  target: Button | undefined | null,
};

const StyledForm = styled.form`
  width: 210px;
`;

const stopEvent = (e) => {
  e.preventDefault();
  e.stopPropagation();
};

const SavedSearchForm = (props: Props) => {
  const {
    isCreateNew,
    disableCreateNew,
    onChangeTitle,
    saveSearch,
    saveAsSearch,
    toggleModal,
    value,
    target,
  } = props;
  const disableSaveAs = !value || value === '' || disableCreateNew;
  const createNewTitle = isCreateNew ? 'Create new' : 'Save as';
  const pluggableSaveViewControls = useSaveViewFormControls();

  return (
    <Portal>
      <Position placement="left"
                target={target}>
        <Popover title="Name of search" id="saved-search-popover">
          <StyledForm onSubmit={stopEvent}>
            <FormGroup>
              <ControlLabel htmlFor="title">Title</ControlLabel>
              <FormControl type="text"
                           value={value}
                           id="title"
                           placeholder="Enter title"
                           onChange={onChangeTitle} />
            </FormGroup>
            {pluggableSaveViewControls?.map(({ component: Component, id }) => (Component && <Component key={id} disabledViewCreation={disableSaveAs} />))}
            <ButtonToolbar>
              {!isCreateNew && (
                <Button bsStyle="primary"
                        className={styles.button}
                        type="submit"
                        bsSize="sm"
                        onClick={saveSearch}>
                  Save
                </Button>
              )}
              <Button disabled={disableSaveAs}
                      bsStyle="info"
                      className={styles.button}
                      type="submit"
                      bsSize="sm"
                      onClick={saveAsSearch}>
                {createNewTitle}
              </Button>
              <Button className={styles.button}
                      onClick={toggleModal}
                      bsSize="sm">
                Cancel
              </Button>
            </ButtonToolbar>
          </StyledForm>
        </Popover>
      </Position>
    </Portal>
  );
};

export default SavedSearchForm;
