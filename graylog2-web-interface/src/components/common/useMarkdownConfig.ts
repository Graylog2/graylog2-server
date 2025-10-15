import useClusterConfig from 'hooks/useClusterConfig';
import type { MarkdownConfigType } from 'components/common/types';

const CONFIG_CLASS = 'org.graylog2.configuration.MarkdownConfiguration';

const useMarkdownConfig = () => useClusterConfig<MarkdownConfigType>(CONFIG_CLASS);
export default useMarkdownConfig;
