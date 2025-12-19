import usePluginEntities from 'hooks/usePluginEntities';
import type { PageContext } from 'views/types';

const noop = (_context: PageContext) => {};
const usePageContext = (context: PageContext) => {
  const useContextExtension = usePluginEntities('assistant.useContextExtension')[0] ?? noop;

  useContextExtension(context);
};

export default usePageContext;
