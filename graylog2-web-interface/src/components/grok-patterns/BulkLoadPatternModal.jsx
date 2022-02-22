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
import React from 'react';

import { Button, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { GrokPatternsStore } from 'stores/grok-patterns/GrokPatternsStore';

class BulkLoadPatternModal extends React.Component {
  static propTypes = {
    onSuccess: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      dropExistingPatterns: false,
    };
  }

  _onSubmit = (evt) => {
    evt.preventDefault();

    const reader = new FileReader();
    const { dropExistingPatterns } = this.state;
    const { onSuccess } = this.props;

    reader.onload = (loaded) => {
      const request = loaded.target.result;

      GrokPatternsStore.bulkImport(request, dropExistingPatterns).then(() => {
        UserNotification.success('Grok Patterns imported successfully', 'Success!');
        this.modal.close();
        onSuccess();
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
          <Input id="drop-existing-patterns-checkbox"
                 type="checkbox"
                 name="drop-existing"
                 label="Drop all existing patterns before import?"
                 onChange={(e) => this.setState({ dropExistingPatterns: e.target.checked })} />
        </BootstrapModalForm>
      </span>
    );
  }
}

export default BulkLoadPatternModal;
