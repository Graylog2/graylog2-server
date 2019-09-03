// @flow strict
import React from 'react';
import { Button, ControlLabel, FormControl, FormGroup, Popover } from 'react-bootstrap';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

type Props = {
  // eslint-disable-next-line no-undef
  onChangeTitle: (SyntheticInputEvent<HTMLInputElement>) => void,
  saveSearch: () => void,
  saveAsSearch: () => void,
  disableCreateNew: boolean,
  toggleModal: () => void,
  isCreateNew: boolean,
  value: string,
  target: any,
};

const BookmarkForm = (props: Props) => {
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
        <Popover title="Name of search" id="bookmark-popover">
          <form>
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
                      type="submit"
                      onClick={saveSearch}>
                Save
              </Button>
            )}
            <Button disabled={disableSaveAs}
                    bsStyle="info"
                    type="submit"
                    onClick={saveAsSearch}>
              {createNewTitle}
            </Button>
            <Button onClick={toggleModal}>Cancel</Button>
          </form>
        </Popover>
      </Position>
    </Portal>
  );
};

export default BookmarkForm;
