// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { ActionContext } from 'enterprise/logic/ActionContext';

// $FlowFixMe: imports from core need to be fixed in flow
import ExtractorUtils from 'util/ExtractorUtils';

// $FlowFixMe: imports from core need to be fixed in flow
import Select from 'components/common/Select';

// $FlowFixMe: imports from core need to be fixed in flow
import { BootstrapModalForm } from 'components/bootstrap';

type Props = {
  onClose: () => void,
  value: any,
  field: any,
};

type State = {
  selectedExtractor: ?string,
};

class SelectExtractorType extends React.Component<Props, State> {
  static propTypes = {
    onClose: PropTypes.func.isRequired,
  };

  static contextType = ActionContext;

  state = {
    selectedExtractor: undefined,
  };

  componentDidMount() {
    /* eslint-disable-next-line camelcase */
    const { gl2_source_node, gl2_source_input } = this.context.message.fields;
    this.extractorRoutes = ExtractorUtils.getNewExtractorRoutes(gl2_source_node,
      gl2_source_input, this.props.field, this.context.message.index, this.context.message.id);
  }

  extractorRoutes = {};
  /* eslint-disable-next-line react/no-unused-prop-types */
  _renderOption = ({ label }: { label: string }) => <React.Fragment><strong>{label}</strong></React.Fragment>;

  _onSubmit = () => {
    this.props.onClose();

    if (this.state.selectedExtractor) {
      const uri = this.extractorRoutes[this.state.selectedExtractor];
      const newWindow = window.open(uri, '_blank');
      newWindow.focus();
    }
  };

  _getExtractorTypes = () => {
    return ExtractorUtils.EXTRACTOR_TYPES.map((extractorType) => {
      return { label: ExtractorUtils.getReadableExtractorTypeName(extractorType), value: extractorType };
    });
  };

  _onChange = (selectedExtractor: string) => {
    this.setState({ selectedExtractor });
  };

  render() {
    return (
      <BootstrapModalForm title="Select extractor type"
                          submitButtonDisabled={!this.state.selectedExtractor}
                          show
                          onSubmitForm={this._onSubmit}>
        <Select
          placeholder="Select extractor type"
          optionRenderer={this._renderOption}
          clearable
          onChange={this._onChange}
          options={this._getExtractorTypes()}
        />
      </BootstrapModalForm>);
  }
}

export default SelectExtractorType;
