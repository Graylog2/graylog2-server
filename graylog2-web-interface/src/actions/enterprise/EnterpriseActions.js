import Reflux from 'reflux';

const EnterpriseActions = Reflux.createActions({
  requestFreeEnterpriseLicense: { asyncResult: true },
  getLicenseInfo: { asyncResult: true },
});

export default EnterpriseActions;
