import React from 'react';
import { Button } from 'react-bootstrap';
import { markdown } from 'markdown';

import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';
const ConfigurationBundlesActions = ActionsProvider.getActions('ConfigurationBundles');

const ConfigurationBundlePreview = React.createClass({
  _confirmDeletion() {
    if (window.confirm('You are about to delete this content pack, are you sure?')) {
      ConfigurationBundlesActions.delete(this.props.sourceTypeId).then(() => {
        UserNotification.success('Bundle deleted successfully!', 'Success!');
      }, () => {
        UserNotification.error('Deleting bundle failed!', 'Error!');
      });
    }
  },
  _onApply() {
    ConfigurationBundlesActions.apply(this.props.sourceTypeId).then(() => {
      UserNotification.success('Bundle applied successfully!', 'Success!');
    }, () => {
      UserNotification.error('Applying bundle failed!', 'Error!');
    });
  },
  render() {
    let preview = 'Select a content pack from the list to see its preview.';
    let apply_action = '';
    let delete_action = '';

    if (this.props.sourceTypeDescription) {
      preview = this.props.sourceTypeDescription;
      apply_action = <Button bsStyle="success" onClick={this._onApply}>Apply content</Button>;
      delete_action = <Button className="pull-right" bsStyle="warning" bsSize="xsmall" onClick={this._confirmDeletion}>Remove pack</Button>;
    }

    const markdownPreview = markdown.toHTML(preview);

    return (
      <div className="bundle-preview">
        <div style={{ marginBottom: 5 }}>
          {delete_action}
          <h2>Content pack description:</h2>
        </div>
        <div dangerouslySetInnerHTML={{__html: markdownPreview}}/>
        <div className="preview-actions">
          {apply_action}
        </div>
      </div>
    );
  },
});

export default ConfigurationBundlePreview;
