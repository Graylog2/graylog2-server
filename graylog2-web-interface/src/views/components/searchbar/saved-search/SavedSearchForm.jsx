// @flow strict
import React from 'react';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';
import styled from 'styled-components';

import { Button, ControlLabel, FormControl, FormGroup, Popover } from 'components/graylog';

import styles from './SavedSearchForm.css';

type Props = {
  onChangeTitle: (SyntheticInputEvent<HTMLInputElement>) => void,
  saveSearch: () => void,
  saveAsSearch: () => void,
  disableCreateNew: boolean,
  toggleModal: () => void,
  isCreateNew: boolean,
  value: string,
  target: ?Button,
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

  return (
    <Portal>
      <Position container={document.body}
                placement="left"
                target={target}>
        <Popover title="Name of search" id="saved-search-popover">
          <StyledForm onSubmit={stopEvent}>
            <FormGroup>
              <ControlLabel>Title</ControlLabel>
              <FormControl type="text"
                           value={value}
                           placeholder="Enter title"
                           onChange={onChangeTitle} />
            </FormGroup>
            {!isCreateNew
            && (
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
          </StyledForm>
        </Popover>
      </Position>
    </Portal>
  );
};

export default SavedSearchForm;
