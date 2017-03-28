const fs = require('fs');

function linkTagForCssFile(cssFile) {
  return '<link href="' + cssFile + '" rel="stylesheet">';
}

function scriptTagForJsFile(jsFile) {
  return '<script src="' + jsFile + '"></script>';
}

module.exports = function (templateParams) {
  const htmlWebpackPlugin = templateParams.htmlWebpackPlugin;
  const vendorModule = JSON.parse(fs.readFileSync(htmlWebpackPlugin.options.vendorModulePath, 'utf8'));
  const manifest = htmlWebpackPlugin.files.manifest ? ' manifest="' + htmlWebpackPlugin.files.manifest + '"' : '';
  const title = htmlWebpackPlugin.options.title ? htmlWebpackPlugin.options.title : 'Webpack App';
  const favIcon = htmlWebpackPlugin.files.favicon ? '<link rel="shortcut icon" href="' + htmlWebpackPlugin.files.favicon + '">' : '';

  const cssFiles = htmlWebpackPlugin.files.css.map(function(css) {
    return linkTagForCssFile(htmlWebpackPlugin.files.css[css]);
  }).join('\n');

  const vendorFiles = Object.keys(vendorModule.files.chunks).map(function(chunk) {
    return '\t' + scriptTagForJsFile('/' + vendorModule.files.chunks[chunk].entry);
  });
  const jsFiles = Object.keys(htmlWebpackPlugin.files.chunks).map(function(chunk) {
    return '\t' + scriptTagForJsFile(htmlWebpackPlugin.files.chunks[chunk].entry);
  }).join('\n');

  return '\
  <!DOCTYPE html>\n\
  <html' + manifest + '>\n\
    <head>\n\
      <meta charset="UTF-8">\n\
      <title>' + title + ' </title>\n\
      ' + favIcon + '\n\
      ' + cssFiles + '\n\
    </head>\n\
    <body>\n\
      <script src="/config.js"></script>\n\
      ' + vendorFiles + '\n\
      ' + jsFiles + '\n\
    </body>\n\
  </html>\n';
};
