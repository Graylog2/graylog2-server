// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import Version from 'util/Version';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

type Props = {
  system?: {
    version: string,
    hostname: string
  },
};

const Footer = ({ system }: Props) => {
  const [jvm, setJvm] = useState();
  useEffect(() => {
    let mounted = true;
    SystemStore.jvm().then((jvmInfo) => {
      if (mounted) {
        setJvm(jvmInfo);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  if (!(system && jvm)) {
    return (
      <div id="footer">
        Graylog {Version.getFullVersion()}
      </div>
    );
  }

  return (
    <div id="footer">
      Graylog {system.version} on {system.hostname} ({jvm.info})
    </div>
  );
};

Footer.propTypes = {
  system: PropTypes.shape({
    version: PropTypes.string,
    hostname: PropTypes.string,
  }),
};

Footer.defaultProps = {
  system: undefined,
};

export default connect(Footer, { system: SystemStore }, ({ system: { system } = {} }) => ({ system }));
