import React from 'react';
import { Input, Button } from 'react-bootstrap';

import UserNotification from 'util/UserNotification';

import GrokPatternsStore from 'stores/grok-patterns/GrokPatternsStore';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const BulkLoadPatternModal = React.createClass({
  propTypes: {
    onSuccess: React.PropTypes.func.isRequired,
  },
  _onSubmit(evt) {
    evt.preventDefault();

    const reader = new FileReader();
    const replaceAll = this.refs['replace-patterns'].checked;

    reader.onload = (loaded) => {
      const request = loaded.target.result.split('\n').map((line) => {
        if (!line.startsWith('#')) {
          const splitted = line.split(/\s+/, 2);
          if (splitted.length > 1) {
            return {name: splitted[0], pattern: splitted[1]};
          }
        }
      }).filter((elem) => elem !== undefined);
      GrokPatternsStore.bulkImport(request, replaceAll).then(() => {
        UserNotification.success('Grok Patterns imported successfully', 'Success!');
        this.refs.modal.close();
        this.props.onSuccess();
      });
    };

    reader.readAsText(this.refs['pattern-file'].getInputDOMNode().files[0]);
  },
  render() {
    return (
      <span>
        <Button bsStyle="info" style={{marginRight: 5}} onClick={() => this.refs.modal.open()}>Import pattern file</Button>

          <BootstrapModalForm ref="modal"
                              title="Import Grok patterns from file"
                              submitButtonText="Upload"
                              formProps={{onSubmit: this._onSubmit}}>
            <Input type="file"
                   ref="pattern-file"
                   name="patterns"
                   label="Pattern file"
                   help="A file containing Grok patterns, one per line. Name and patterns should be separated by whitespace."
                   required />
            <Input type="checkbox"
                   ref="replace-patterns"
                   name="replace"
                   label="Replace all existing patterns?" />
          </BootstrapModalForm>
      </span>
    );
  },
});

export default BulkLoadPatternModal;
