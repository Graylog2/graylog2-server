const awsAuth = ({ awsCloudWatchAwsKey, awsCloudWatchAwsSecret }) => {
  /*
    For development, create a sibling file named `awsKeySecret.js` in the current directory.
    ```module.exports = { key: 'YOUR_REAL_KEY', secret: 'YOUR_REAL_SECRET' };```
    This file is already set in .gitignore so it won't be commited
  */

  const key = awsCloudWatchAwsKey ? awsCloudWatchAwsKey.value : '';
  const secret = awsCloudWatchAwsSecret ? awsCloudWatchAwsSecret.value : '';
  const auth = { key, secret };

  try {
    // eslint-disable-next-line global-require
    const realAuth = require('./awsKeySecret');
    return { ...auth, ...realAuth };
  } catch (e) {
    return auth;
  }
};

export default awsAuth;
