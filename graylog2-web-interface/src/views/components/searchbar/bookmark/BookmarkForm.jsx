// @flow strict
import React from 'react';
import { Button, Popover, ControlLabel, FormControl, FormGroup } from 'react-bootstrap';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

type Props = {
  // eslint-disable-next-line no-undef
  onChangeTitle: (SyntheticInputEvent<HTMLInputElement>) => void,
  saveSearch: () => void,
  saveAsSearch: () => void,
  toggleModal: () => void,
  hideSave: boolean,
  value: string,
  target: any,
}

class BookmarkForm extends React.Component<Props> {
  render() {
    const {
      hideSave,
      onChangeTitle,
      saveSearch,
      saveAsSearch,
      toggleModal,
      value,
      target
    } = this.props;
    const disableSaveAs = !value || value === '';
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
              {!hideSave
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
                Create new
              </Button>
              <Button onClick={toggleModal}>Cancel</Button>
            </form>
          </Popover>
        </Position>
      </Portal>
    );
  }
}

export default BookmarkForm;
