import React, { useState } from 'react';
import PropTypes from 'prop-types';

import KinesisStreams from './KinesisStreams';
import KinesisSetup from './KinesisSetup';
import { AdvancedOptionsProvider } from './context/AdvancedOptions';

const StepKinesis = ({ hasStreams, ...restProps }) => {
  const [renderStreams, toggleRenderStreams] = useState(hasStreams);

  return (
    <AdvancedOptionsProvider>
      { renderStreams
        ? <KinesisStreams {...restProps} toggleSetup={() => toggleRenderStreams(false)} />
        : <KinesisSetup {...restProps} toggleSetup={hasStreams ? () => toggleRenderStreams(true) : null} />
      }
    </AdvancedOptionsProvider>
  );
};

StepKinesis.propTypes = {
  hasStreams: PropTypes.bool,
  onChange: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
};

StepKinesis.defaultProps = {
  hasStreams: false,
};

export default StepKinesis;
