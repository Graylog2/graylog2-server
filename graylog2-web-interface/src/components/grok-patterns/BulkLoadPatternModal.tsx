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
import React from 'react';
import type { WithTranslation } from 'react-i18next';
import { withTranslation } from 'react-i18next';

import { Button, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { bulkImportGrokPatterns } from 'hooks/useGrokPatterns';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type BulkLoadPatternModalProps = {
  onSuccess: (...args: any[]) => void;
  sendTelemetry?: (...args: any[]) => void;
} & WithTranslation;

class BulkLoadPatternModal extends React.Component<
  BulkLoadPatternModalProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    sendTelemetry: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
      importStrategy: 'ABORT_ON_CONFLICT',
    };
  }

  private patternFile: Input;

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

      bulkImportGrokPatterns(request as string, importStrategy).then(() => {
        UserNotification.success(this.props.t('import_success'), this.props.t('import_success_title'));
        this._closeModal();

        this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.GROK_PATTERN.IMPORTED, {
          app_pathname: 'grokpatterns',
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
        <Button bsStyle="info" style={{ marginRight: 5 }} onClick={this._openModal}>
          {this.props.t('button_import_file')}
        </Button>

        <BootstrapModalForm
          show={this.state.showModal}
          title={this.props.t('modal_import_title')}
          submitButtonText={this.props.t('button_upload')}
          onCancel={this._closeModal}
          onSubmitForm={this._onSubmit}>
          <Input
            id="pattern-file"
            type="file"
            ref={(patternFile) => {
              this.patternFile = patternFile;
            }}
            name="patterns"
            label={this.props.t('input_file_label')}
            help={this.props.t('input_file_help')}
            required
          />
          <Input
            id="abort-on-conflicting-patterns-radio"
            type="radio"
            name="import-strategy"
            value="ABORT_ON_CONFLICT"
            label={this.props.t('import_strategy_abort')}
            defaultChecked
            onChange={(e) => this._onImportStrategyChange(e)}
          />
          <Input
            id="replace-conflicting-patterns-radio"
            type="radio"
            name="import-strategy"
            value="REPLACE_ON_CONFLICT"
            label={this.props.t('import_strategy_replace')}
            onChange={(e) => this._onImportStrategyChange(e)}
          />
          <Input
            id="drop-existing-patterns-radio"
            type="radio"
            name="import-strategy"
            value="DROP_ALL_EXISTING"
            label={this.props.t('import_strategy_drop')}
            onChange={(e) => this._onImportStrategyChange(e)}
          />
        </BootstrapModalForm>
      </span>
    );
  }
}

export default withTelemetry(withTranslation('grokpatterns')(BulkLoadPatternModal), 'grokpatterns');
