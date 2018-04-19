import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';

import StoreProvider from 'injection/StoreProvider';
const GrokPatternsStore = StoreProvider.getStore('GrokPatterns');

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

class BulkLoadPatternModal extends React.Component {
  static propTypes = {
    onSuccess: PropTypes.func.isRequired,
  };

  state = {
    replacePatterns: false,
  };

  _onSubmit = (evt) => {
    evt.preventDefault();

    const reader = new FileReader();

    reader.onload = (loaded) => {
      const request = loaded.target.result;
      GrokPatternsStore.bulkImport(request, this.state.replacePatterns).then(() => {
        UserNotification.success('Grok Patterns imported successfully', 'Success!');
        this.modal.close();
        this.props.onSuccess();
      });
    };

    reader.readAsText(this.patternFile.getInputDOMNode().files[0]);
  };

  render() {
    return (
      <span>
        <Button bsStyle="info" style={{ marginRight: 5 }} onClick={() => this.modal.open()}>Import pattern file</Button>

        <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                              title="Import Grok patterns from file"
                              submitButtonText="Upload"
                              formProps={{ onSubmit: this._onSubmit }}>
          <Input id="pattern-file"
                 type="file"
                 ref={(patternFile) => { this.patternFile = patternFile; }}
                 name="patterns"
                 label="Pattern file"
                 help="A file containing Grok patterns, one per line. Name and patterns should be separated by whitespace."
                 required />
          <Input id="replace-patterns-checkbox"
                 type="checkbox"
                 name="replace"
                 label="Replace all existing patterns?"
                 onChange={e => this.setState({ replacePatterns: e.target.checked })}
          />
        </BootstrapModalForm>
      </span>
    );
  }
}

export default BulkLoadPatternModal;
