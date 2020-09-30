// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import Panel from 'components/graylog/Panel';

type Props = {
  pluginType: string,
};

const MissingEnterprisePlugin = ({ pluginType }: Props) => (
  <Panel bsStyle="danger">
    <Panel.Body>
      <p>
        <b>No Enterprise Plugin Found</b>
      </p>
      To use the{pluginType && <> <b>{pluginType}</b> </>} functionality you need to install the Graylog <b>Enterprise</b> plugin.
    </Panel.Body>
  </Panel>
);

MissingEnterprisePlugin.propTypes = {
  pluginType: PropTypes.string,
};

MissingEnterprisePlugin.defaultProps = {
  pluginType: undefined,
};

export default MissingEnterprisePlugin;
