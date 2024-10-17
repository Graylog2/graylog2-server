import React from 'react';

import { Button, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { GrokPatternsStore } from 'stores/grok-patterns/GrokPatternsStore';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type BulkLoadPatternModalProps = {
  onSuccess: (...args: any[]) => void;
  sendTelemetry?: (...args: any[]) => void;
};

class BulkLoadPatternModal extends React.Component<BulkLoadPatternModalProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    sendTelemetry: () => {},
  };

  private patternFile: Input;

  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
      importStrategy: 'ABORT_ON_CONFLICT',
    };
  }

  _openModal = () => {
    this.setState({ showModal: true });
  };

  _closeModal = () => {
    this.setState({ importStrategy: 'ABORT_ON_CONFLICT', showModal: false });
  };

  _onSubmit = (evt) => {
    evt.preventDefault();

    const reader = new FileReader();
    const { importStrategy } = this.state;
    const { onSuccess } = this.props;

    reader.onload = (loaded) => {
      const request = loaded.target.result;

      GrokPatternsStore.bulkImport(request, importStrategy).then(() => {
        UserNotification.success('Grok Patterns imported successfully', 'Success!');
        this._closeModal();

        this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.GROK_PATTERN.IMPORTED, {
          app_pathname: 'grokpatterns',
          app_section: 'grokpatterns',
        });

        onSuccess();
      });
    };

    reader.readAsText(this.patternFile.getInputDOMNode().files[0]);
  };

  _onImportStrategyChange = (event) => this.setState({ importStrategy: event.target.value });

  render() {
    return (
      <span>
        <Button bsStyle="info" style={{ marginRight: 5 }} onClick={this._openModal}>Import pattern file</Button>

        <BootstrapModalForm show={this.state.showModal}
                            title="Import Grok patterns from file"
                            submitButtonText="Upload"
                            onCancel={this._closeModal}
                            onSubmitForm={this._onSubmit}>
          <Input id="pattern-file"
                 type="file"
                 ref={(patternFile) => { this.patternFile = patternFile; }}
                 name="patterns"
                 label="Pattern file"
                 help="A file containing Grok patterns, one per line. Name and patterns should be separated by whitespace."
                 required />
          <Input id="abort-on-conflicting-patterns-radio"
                 type="radio"
                 name="import-strategy"
                 value="ABORT_ON_CONFLICT"
                 label="Abort import if a pattern with the same name already exists"
                 defaultChecked
                 onChange={(e) => this._onImportStrategyChange(e)} />
          <Input id="replace-conflicting-patterns-radio"
                 type="radio"
                 name="import-strategy"
                 value="REPLACE_ON_CONFLICT"
                 label="Replace existing patterns with the same name"
                 onChange={(e) => this._onImportStrategyChange(e)} />
          <Input id="drop-existing-patterns-radio"
                 type="radio"
                 name="import-strategy"
                 value="DROP_ALL_EXISTING"
                 label="Drop all existing patterns before import"
                 onChange={(e) => this._onImportStrategyChange(e)} />
        </BootstrapModalForm>
      </span>
    );
  }
}

export default withTelemetry(BulkLoadPatternModal);
