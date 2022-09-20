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
import PropTypes from 'prop-types';
import * as React from 'react';
import { useCallback, useRef } from 'react';

// eslint-disable-next-line no-restricted-imports
import Routes from 'routing/Routes';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { StartpageStore } from 'stores/users/StartpageStore';
import type { Stream } from 'stores/streams/StreamsStore';
import { LinkContainer } from 'components/common/router';

import StreamForm from './StreamForm';

type Props = {
  stream: Stream,
  user: { id: string, read_only: boolean },
  indexSets: Array<{}>,
  onDelete: (stream: Stream) => void,
  onClone: (streamId: string, newStream: Stream) => void,
  onQuickAdd: (streamId: string) => void,
  onUpdate: () => void,
  isDefaultStream?: boolean,
  disabled?: boolean,
};

type OpenableForm = {
  open: () => void
}

const StreamControls = ({
  stream,
  disabled,
  isDefaultStream,
  user,
  onClone,
  onDelete,
  onQuickAdd,
  onUpdate,
  indexSets,
}: Props) => {
  const streamForm = useRef<OpenableForm>();
  const cloneForm = useRef<OpenableForm>();
  const _onDelete = useCallback(() => onDelete(stream), [onDelete, stream]);
  const _onEdit = useCallback(() => streamForm.current.open(), []);
  const _onClone = useCallback(() => cloneForm.current.open(), []);
  const _onCloneSubmit = useCallback((_, newStream: Stream) => onClone(stream.id, newStream), [onClone, stream.id]);
  const _onQuickAdd = useCallback(() => onQuickAdd(stream.id), [onQuickAdd, stream.id]);
  const _setStartpage = useCallback(() => StartpageStore.set(user.id, 'stream', stream.id), [stream.id, user.id]);

  return (
    <>
      <DropdownButton title="More Actions"
                      pullRight
                      id={`more-actions-dropdown-${stream.id}`}
                      disabled={disabled}>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem key={`editStreams-${stream.id}`} onSelect={_onEdit} disabled={isDefaultStream}>
            Edit stream
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem key={`quickAddRule-${stream.id}`} onSelect={_onQuickAdd} disabled={isDefaultStream}>
            Quick add rule
          </MenuItem>
        </IfPermitted>
        <IfPermitted permissions={['streams:create', `streams:read:${stream.id}`]}>
          <MenuItem key={`cloneStream-${stream.id}`} onSelect={_onClone} disabled={isDefaultStream}>
            Clone this stream
          </MenuItem>
        </IfPermitted>
        <HideOnCloud>
          <IfPermitted permissions="stream_outputs:read">
            <LinkContainer to={Routes.stream_outputs(stream.id)}>
              <MenuItem key={`manageOutputs-${stream.id}`}>
                Manage Outputs
              </MenuItem>
            </LinkContainer>
          </IfPermitted>
        </HideOnCloud>
        <MenuItem key={`setAsStartpage-${stream.id}`} onSelect={_setStartpage} disabled={user.read_only}>
          Set as startpage
        </MenuItem>

        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem key={`divider-${stream.id}`} divider />
        </IfPermitted>
        <IfPermitted permissions={`streams:edit:${stream.id}`}>
          <MenuItem key={`deleteStream-${stream.id}`} onSelect={_onDelete} disabled={isDefaultStream}>
            Delete this stream
          </MenuItem>
        </IfPermitted>
      </DropdownButton>
      <StreamForm ref={streamForm}
                  title="Editing Stream"
                  onSubmit={onUpdate}
                  submitButtonText="Update stream"
                  stream={stream}
                  indexSets={indexSets} />
      <StreamForm ref={cloneForm}
                  title="Cloning Stream"
                  onSubmit={_onCloneSubmit}
                  submitButtonText="Clone stream"
                  indexSets={indexSets} />
    </>
  );
};

StreamControls.propTypes = {
  stream: PropTypes.object.isRequired,
  user: PropTypes.object.isRequired,
  indexSets: PropTypes.array.isRequired,
  onDelete: PropTypes.func.isRequired,
  onClone: PropTypes.func.isRequired,
  onQuickAdd: PropTypes.func.isRequired,
  onUpdate: PropTypes.func.isRequired,
  isDefaultStream: PropTypes.bool,
  disabled: PropTypes.bool,
};

StreamControls.defaultProps = {
  disabled: false,
  isDefaultStream: false,
};

export default StreamControls;
