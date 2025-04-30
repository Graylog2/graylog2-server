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
import EntityCreateShareFormGroup from 'components/permissions/EntityCreateShareFormGroup';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import styles from './SavedSearchForm.css';


type Props = React.PropsWithChildren<{
  show: boolean;
  saveSearch: (newTitle: string, entityShare?: EntitySharePayload) => void;
  saveAsSearch: (newTitle: string,  entityShare?: EntitySharePayload) => void;
  toggleModal: () => void;
  isCreateNew: boolean;
  value: string;
  viewId?: string;
}>;

const stopEvent = (e) => {
  e.preventDefault();
  e.stopPropagation();
};
const StyledPopoverDropdown = styled(Popover.Dropdown)`
`;

const SavedSearchForm = ({ children = undefined, show, isCreateNew, saveSearch, saveAsSearch, toggleModal, value, viewId = null }: Props) => {
  const [title, setTitle] = useState(value);
  const [sharePayload, setSharePayload] = useState(null);
  const onChangeTitle = useCallback(
    (e: React.FormEvent<unknown>) => setTitle((e.target as HTMLInputElement).value),
    [],
  );

  const trimmedTitle = (title ?? '').trim();
  const disableSaveAs = trimmedTitle === '' || (!isCreateNew && trimmedTitle === value);
  const createNewTitle = isCreateNew ? 'Create new' : 'Save as';
  const createNewButtonTitle = isCreateNew ? 'Create new search' : 'Save as new search';
  const pluggableSaveViewControls = useSaveViewFormControls();
  const _saveSearch = useCallback(() => saveSearch(title, sharePayload), [saveSearch, title, sharePayload]);
  const _saveAsSearch = useCallback(() => saveAsSearch(title, sharePayload), [saveAsSearch, title, sharePayload]);

  return (
    <Popover position="left" width={500} opened={show} withArrow withinPortal>
      <Popover.Target>{children}</Popover.Target>
      <StyledPopoverDropdown title="Name of search" id="saved-search-popover">
        <form onSubmit={stopEvent}>
          <FormGroup>
            <ControlLabel htmlFor="title">Title</ControlLabel>
            <FormControl type="text" value={title} id="title" placeholder="Enter title" onChange={onChangeTitle} />
          </FormGroup>
          {pluggableSaveViewControls?.map(
            ({ component: Component, id }) => Component && <Component key={id} disabledViewCreation={disableSaveAs} />,
          )}
          <EntityCreateShareFormGroup
            description='Search for a User or Team to add as collaborator on this search.'
            entityType='search'
            entityTitle=''
            entityId={isCreateNew ? null: viewId}
            onSetEntityShare={(payload) => setSharePayload(payload)}
          />
          <ButtonToolbar>
            {!isCreateNew && (
              <Button
                bsStyle="primary"
                className={styles.button}
                type="submit"
                bsSize="sm"
                title="Save search"
                onClick={_saveSearch}>
                Save
              </Button>
            )}
            <Button
              disabled={disableSaveAs}
              bsStyle="info"
              className={styles.button}
              type="submit"
              bsSize="sm"
              title={createNewButtonTitle}
              onClick={_saveAsSearch}>
              {createNewTitle}
            </Button>
            <Button className={styles.button} onClick={toggleModal} bsSize="sm">
              Cancel
            </Button>
          </ButtonToolbar>
        </form>
      </StyledPopoverDropdown>
    </Popover>
  );
};

export default SavedSearchForm;
