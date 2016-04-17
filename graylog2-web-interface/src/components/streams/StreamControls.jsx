import React, {PropTypes} from 'react';
import { DropdownButton, MenuItem } from 'react-bootstrap';

import { IfPermitted } from 'components/common';
import StreamForm from './StreamForm';
import PermissionsMixin from 'util/PermissionsMixin';

import StoreProvider from 'injection/StoreProvider';
const StartpageStore = StoreProvider.getStore('Startpage');

const StreamControls = React.createClass({
  propTypes: {
    stream: PropTypes.object.isRequired,
    user: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
    onClone: PropTypes.func.isRequired,
    onQuickAdd: PropTypes.func.isRequired,
    onUpdate: PropTypes.func.isRequired,
  },
  mixins: [PermissionsMixin],
  getInitialState() {
    return {};
  },
  _onDelete(event) {
    event.preventDefault();
    this.props.onDelete(this.props.stream);
  },
  _onEdit(event) {
    event.preventDefault();
    this.refs.streamForm.open();
  },
  _onClone(event) {
    event.preventDefault();
    this.refs.cloneForm.open();
  },
  _onCloneSubmit(_, stream) {
    this.props.onClone(this.props.stream.id, stream);
  },
  _onQuickAdd(event) {
    event.preventDefault();
    this.props.onQuickAdd(this.props.stream.id);
  },
  _setStartpage(event) {
    event.preventDefault();
    StartpageStore.set(this.props.user.username, 'stream', this.props.stream.id);
  },
  render() {
    const stream = this.props.stream;

    return (
      <span>
          <DropdownButton title="More actions" ref="dropdownButton" pullRight
                          id={`more-actions-dropdown-${stream.id}`}>
            <IfPermitted permissions={`streams:edit:${stream.id}`}>
              <MenuItem key={`editStreams-${stream.id}`} onSelect={this._onEdit}>Edit stream</MenuItem>
            </IfPermitted>
            <IfPermitted permissions={`streams:edit:${stream.id}`}>
              <MenuItem key={`quickAddRule-${stream.id}`} onSelect={this._onQuickAdd}>Quick add rule</MenuItem>
            </IfPermitted>
            <IfPermitted permissions={['streams:create', `streams:read:${stream.id}`]}>
              <MenuItem key={`cloneStream-${stream.id}`} onSelect={this._onClone}>Clone this stream</MenuItem>
            </IfPermitted>
            <MenuItem key={`setAsStartpage-${stream.id}`} onSelect={this._setStartpage} disabled={this.props.user.read_only}>
              Set as startpage
            </MenuItem>
            <IfPermitted permissions={`streams:edit:${stream.id}`}>
              <MenuItem key={`divider-${stream.id}`} divider/>
            </IfPermitted>
            <IfPermitted permissions={`streams:edit:${stream.id}`}>
              <MenuItem key={`deleteStream-${stream.id}`} onSelect={this._onDelete}>
                Delete this stream
              </MenuItem>
            </IfPermitted>
          </DropdownButton>
          <StreamForm ref="streamForm" title="Editing Stream" onSubmit={this.props.onUpdate} stream={stream}/>
          <StreamForm ref="cloneForm" title="Cloning Stream" onSubmit={this._onCloneSubmit}/>
      </span>
    );
  },
});

export default StreamControls;
