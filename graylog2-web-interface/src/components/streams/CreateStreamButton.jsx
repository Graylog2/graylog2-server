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
