// @flow strict
import { withRouter } from 'react-router';

const withLocation = <T>(Component: T): T => withRouter(Component);

export default withLocation;
