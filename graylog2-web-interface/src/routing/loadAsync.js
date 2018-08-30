import loadable from 'loadable-components';

export default f => loadable(() => f().then(c => c.default));
