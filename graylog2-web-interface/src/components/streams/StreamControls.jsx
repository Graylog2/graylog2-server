import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import Routes from 'routing/Routes';
import { DropdownButton, MenuItem } from 'components/graylog';
import { IfPermitted } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';
import StoreProvider from 'injection/StoreProvider';

import StreamForm from './StreamForm';

const StartpageStore = StoreProvider.getStore('Startpage');

const StreamControls = createReactClass({
  displayName: 'StreamControls',

  propTypes: {
    stream: PropTypes.object.isRequired,
    user: PropTypes.object.isRequired,
    indexSets: PropTypes.array.isRequired,
    onDelete: PropTypes.func.isRequired,
    onClone: PropTypes.func.isRequired,
    onShare: PropTypes.func.isRequired,
    onQuickAdd: PropTypes.func.isRequired,
    onUpdate: PropTypes.func.isRequired,
    isDefaultStream: PropTypes.bool,
  },

  mixins: [PermissionsMixin],

  getDefaultProps() {
    return {
      isDefaultStream: false,
    };
  },

  _onDelete() {
    const { onDelete, stream } = this.props;
    onDelete(stream);
  },

  _onEdit() {
    this.streamForm.open();
  },

  _onClone() {
    this.cloneForm.open();
  },

  _onCloneSubmit(_, stream) {
    const { onClone, stream: propsStream } = this.props;
    onClone(propsStream.id, stream);
  },

  _onQuickAdd() {
    const { onQuickAdd, stream } = this.props;
    onQuickAdd(stream.id);
  },

  _setStartpage() {
    const { user, stream } = this.props;
    StartpageStore.set(user.username, 'stream', stream.id);
  },

  render() {
    const { stream, isDefaultStream, user, onShare, onUpdate, indexSets } = this.props;

    return (
      <span>
        <DropdownButton title="More Actions"
                        pullRight
                        id={`more-actions-dropdown-${stream.id}`}
                        disabled={isDefaultStream}>
          <IfPermitted permissions={`streams:edit:${stream.id}`}>
            <MenuItem key={`editStreams-${stream.id}`} onSelect={this._onEdit}>Edit stream</MenuItem>
          </IfPermitted>
          <IfPermitted permissions={`streams:edit:${stream.id}`}>
            <MenuItem key={`quickAddRule-${stream.id}`} onSelect={this._onQuickAdd}>Quick add rule</MenuItem>
          </IfPermitted>
          <IfPermitted permissions={['streams:create', `streams:read:${stream.id}`]}>
            <MenuItem key={`cloneStream-${stream.id}`} onSelect={this._onClone}>Clone this stream</MenuItem>
          </IfPermitted>
          <IfPermitted permissions="stream_outputs:read">
            <MenuItem key={`manageOutputs-${stream.id}`} href={Routes.stream_outputs(stream.id)}>
              Manage Outputs
            </MenuItem>
          </IfPermitted>
          <MenuItem key={`setAsStartpage-${stream.id}`} onSelect={this._setStartpage} disabled={user.read_only}>
            Set as startpage
          </MenuItem>

          <IfPermitted permissions={`streams:edit:${stream.id}`}>
            <MenuItem key={`divider-${stream.id}`} divider />
          </IfPermitted>
          <IfPermitted permissions={`streams:edit:${stream.id}`}>
            <MenuItem key={`deleteStream-${stream.id}`} onSelect={this._onDelete}>
              Delete this stream
            </MenuItem>
          </IfPermitted>
        </DropdownButton>
        <StreamForm ref={(streamForm) => { this.streamForm = streamForm; }} title="Editing Stream" onSubmit={onUpdate} stream={stream} indexSets={indexSets} />
        <StreamForm ref={(cloneForm) => { this.cloneForm = cloneForm; }} title="Cloning Stream" onSubmit={this._onCloneSubmit} indexSets={indexSets} />
      </span>
    );
  },
});

export default StreamControls;
