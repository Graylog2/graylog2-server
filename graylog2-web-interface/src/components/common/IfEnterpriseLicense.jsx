// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import Panel from 'components/graylog/Panel';
import CombinedProvider from 'injection/CombinedProvider';

import Spinner from './Spinner';

const { EnterpriseActions } = CombinedProvider.get('Enterprise');

type Props = {
  children: string,
  displayLicenseWarning: boolean,
  renderChildrenOnInvalid: boolean,
};

/**
 * Wrapper component which by default renders its children only if there is a valid enterprise license.
 * Provides a `licenseIsValid` prop for its children. E.g. to display a read only version of its children.
 */
const IfEnterpriseLicense = ({ children, renderChildrenOnInvalid, displayLicenseWarning }: Props) => {
  const [licenseIsValid, setLicenseIsValid] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    EnterpriseActions.getLicenseInfo().then((result) => {
      if (result?.free_license_info?.license_status === 'installed') {
        setLicenseIsValid(true);
      }

      setLoading(false);
    });
  }, []);

  if (loading) {
    return <Spinner />;
  }

  const childrenWithLicenseInfo = React.Children.map(children,
    (child) => React.cloneElement(child, { licenseIsValid }));

  return (
    <>
      {(!licenseIsValid && displayLicenseWarning) && (
        <Panel bsStyle="info">
          <Panel.Body>
            <p>
              <b>Graylog Enterprise Feature</b>
            </p>
            A valid enterprise license is needed to use teams.<br />
            Go to the <Link to={Routes.pluginRoute('SYSTEM_LICENSES')}>Licenses page</Link> for
            more information or contact your Graylog account manager.
          </Panel.Body>
        </Panel>
      )}
      {(licenseIsValid || renderChildrenOnInvalid) && childrenWithLicenseInfo}
    </>
  );
};

IfEnterpriseLicense.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  displayLicenseWarning: PropTypes.bool,
  renderChildrenOnInvalid: PropTypes.bool,
};

IfEnterpriseLicense.defaultProps = {
  /** Allows rendering the children when there is no license, e.g. to display a read only version */
  displayLicenseWarning: true,
  renderChildrenOnInvalid: false,
};

export default IfEnterpriseLicense;
