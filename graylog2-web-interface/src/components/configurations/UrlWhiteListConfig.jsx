// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Button, Table } from 'components/graylog';
import { IfPermitted, Select, Icon } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';
import UrlWhiteListForm from 'components/configurations/UrlWhiteListForm';

type Url = {
  value: string,
  type: string,
};

type Config = {
  entries: Array<Url>
};

type State = {
  config: Config
};

type Props = {
  config: Config,
  updateConfig: (config: Config) => Promise<void>,
};
class UrlWhiteListConfig extends React.Component<Props, State> {
  configModal: ?BootstrapModalForm;

  inputs = {};

  constructor(props: Props) {
    super(props);
    const { config } = this.props;
    this.state = {
      config,
    };
  }

  _summary = (): React.Element<'tr'>[] => {
    const { config: { entries } } = this.props;
    return entries.map((urlConfig, idx) => {
      return (
        <tr>
          <td>{idx + 1}</td>
          <td>{urlConfig.value}</td>
          <td>{urlConfig.type}</td>
        </tr>
      );
    });
  }

  _openModal = () => {
    this.configModal.open();
  }

  _closeModal = () => {
    this.configModal.close();
  }

  _saveConfig = () => {
    const { config } = this.state;
    console.log(config);
    const { updateConfig } = this.props;
    updateConfig(config).then(() => {
      this._closeModal();
    });
  }

  _update = (urls: Array) => {
    this.setState({ config: { entries: urls } });
  }


  _resetConfig = () => {
    const { config } = this.props;
    this.setState({
      config,
    });
  }


  render() {
    const { config: { entries } } = this.props;
    return (
      <div>
        <h2>URL Whitelist Configuration</h2>

        <Table striped bordered condensed className="top-margin">
          <thead>
            <tr>
              <th>#</th>
              <th>URL</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            {this._summary()}
          </tbody>
        </Table>
        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(configModal) => { this.configModal = configModal; }}
                            title="Update White List Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">

          <h3>Urls</h3>
          <UrlWhiteListForm urls={entries} update={this._update} />
        </BootstrapModalForm>
      </div>
    );
  }
}

UrlWhiteListConfig.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default UrlWhiteListConfig;
