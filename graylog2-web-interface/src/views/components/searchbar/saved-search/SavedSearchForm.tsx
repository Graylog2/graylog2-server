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
import { useCallback, useState } from 'react';
import styled from 'styled-components';

import { ButtonToolbar, Button, ControlLabel, FormControl, FormGroup } from 'components/bootstrap';
import Popover from 'components/common/Popover';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';

import styles from './SavedSearchForm.css';

type Props = React.PropsWithChildren<{
  show: boolean,
  saveSearch: (newTitle: string) => void,
  saveAsSearch: (newTitle: string) => void,
  toggleModal: () => void,
  isCreateNew: boolean,
  value: string,
}>;

const StyledForm = styled.form`
  width: 210px;
`;

const stopEvent = (e) => {
  e.preventDefault();
  e.stopPropagation();
};

const SavedSearchForm = ({ children, show, isCreateNew, saveSearch, saveAsSearch, toggleModal, value }: Props) => {
  const [title, setTitle] = useState(value);
  const onChangeTitle = useCallback((e: React.FormEvent<unknown>) => setTitle((e.target as HTMLInputElement).value), []);

  const trimmedTitle = (title ?? '').trim();
  const disableSaveAs = trimmedTitle === '' || (!isCreateNew && trimmedTitle === value);
  const createNewTitle = isCreateNew ? 'Create new' : 'Save as';
  const createNewButtonTitle = isCreateNew ? 'Create new search' : 'Save as new search';
  const pluggableSaveViewControls = useSaveViewFormControls();
  const _saveSearch = useCallback(() => saveSearch(title), [saveSearch, title]);
  const _saveAsSearch = useCallback(() => saveAsSearch(title), [saveAsSearch, title]);

  return (
    <Popover position="left" opened={show} withArrow withinPortal>
      <Popover.Target>
        {children}
      </Popover.Target>
      <Popover.Dropdown title="Name of search"
                        id="saved-search-popover">
        <StyledForm onSubmit={stopEvent}>
          <FormGroup>
            <ControlLabel htmlFor="title">Title</ControlLabel>
            <FormControl type="text"
                         value={title}
                         id="title"
                         placeholder="Enter title"
                         onChange={onChangeTitle} />
          </FormGroup>
          {pluggableSaveViewControls?.map(({ component: Component, id }) => (Component
              && <Component key={id} disabledViewCreation={disableSaveAs} />))}
          <ButtonToolbar>
            {!isCreateNew && (
              <Button bsStyle="primary"
                      className={styles.button}
                      type="submit"
                      bsSize="sm"
                      title="Save search"
                      onClick={_saveSearch}>
                Save
              </Button>
            )}
            <Button disabled={disableSaveAs}
                    bsStyle="info"
                    className={styles.button}
                    type="submit"
                    bsSize="sm"
                    title={createNewButtonTitle}
                    onClick={_saveAsSearch}>
              {createNewTitle}
            </Button>
            <Button className={styles.button}
                    onClick={toggleModal}
                    bsSize="sm">
              Cancel
            </Button>
          </ButtonToolbar>
        </StyledForm>
      </Popover.Dropdown>
    </Popover>
  );
};

export default SavedSearchForm;
