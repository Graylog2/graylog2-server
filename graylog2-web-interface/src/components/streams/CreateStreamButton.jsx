import PropTypes from 'prop-types';
import React from 'react';

import { Button } from 'components/graylog';
import StreamForm from 'components/streams/StreamForm';

class CreateStreamButton extends React.Component {
  static propTypes = {
    buttonText: PropTypes.string,
    bsStyle: PropTypes.string,
    bsSize: PropTypes.string,
    className: PropTypes.string,
    onSave: PropTypes.func.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  static defaultProps = {
    buttonText: 'Create Stream',
    bsSize: undefined,
    bsStyle: undefined,
    className: undefined,
  };

  onClick = () => {
    this.streamForm.open();
  };

  render() {
    const { bsSize, bsStyle, buttonText, className, indexSets, onSave } = this.props;

    return (
      <span>
        <Button bsSize={bsSize}
                bsStyle={bsStyle}
                className={className}
                onClick={this.onClick}>
          {buttonText}
        </Button>
        <StreamForm ref={(streamForm) => { this.streamForm = streamForm; }}
                    title="Creating Stream"
                    indexSets={indexSets}
                    onSubmit={onSave} />
      </span>
    );
  }
}

export default CreateStreamButton;
