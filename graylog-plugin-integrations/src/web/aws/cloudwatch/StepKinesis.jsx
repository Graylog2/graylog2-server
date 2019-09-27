import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { AdvancedOptionsProvider } from 'aws/context/AdvancedOptions';

import ExistingStreams from './kinesis/ExistingStreams';
import SetupNewStream from './kinesis/SetupNewStream';

const StepKinesis = ({ hasStreams, ...restProps }) => {
  const [renderStreams, toggleRenderStreams] = useState(hasStreams);

  return (
    <AdvancedOptionsProvider>
      { renderStreams
        ? <ExistingStreams {...restProps} toggleSetup={() => toggleRenderStreams(false)} />
        : <SetupNewStream {...restProps} toggleSetup={hasStreams ? () => toggleRenderStreams(true) : null} />
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
