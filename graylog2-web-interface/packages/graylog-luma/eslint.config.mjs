import { configs } from 'eslint-plugin-storybook';
import graylog from 'eslint-config-graylog';

export default [...configs['flat/recommended'], ...graylog];
