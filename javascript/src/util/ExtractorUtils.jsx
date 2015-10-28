import jsRoutes from 'routing/jsRoutes';

const ExtractorUtils = {
  EXTRACTOR_TYPES: ['regex', 'substring', 'split_and_index', 'copy_input', 'grok', 'json'],

  getNewExtractorRoutes(sourceNodeId, sourceInputId, fieldName, messageIndex, messageId) {
    const routes = {};
    this.EXTRACTOR_TYPES.forEach(extractorType => {
      routes[extractorType] = jsRoutes.controllers.ExtractorsController.newExtractor(sourceNodeId, sourceInputId, extractorType, fieldName, messageIndex, messageId).url;
    });

    return routes;
  },

  getReadableExtractorTypeName(extractorType) {
    switch (extractorType) {
    case 'regex':
      return 'Regular expression';
    case 'regex_replace':
      return 'Replace with regular expression';
    case 'substring':
      return 'Substring';
    case 'split_and_index':
      return 'Split & Index';
    case 'copy_input':
      return 'Copy Input';
    case 'grok':
      return 'Grok pattern';
    case 'json':
      return 'JSON';
    default:
      return extractorType;
    }
  },
};

export default ExtractorUtils;
