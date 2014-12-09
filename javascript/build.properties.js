module.exports = {
    deployDir: '../app/views/build/',
    deployDirJs: '../app/assets/javascripts/build',
    targetDir: null,
    targetDirJs: null,

    assetDir: 'javascripts/build',
    scriptTemplatePath: 'scripts.hbs',
    scriptDestinationName: 'scripts.scala.html',
    entryPoints: ['./src/mount.jsx'],
    testEntryPoints: ['./test/src/suite.js'],
    browserifyExcludes: ['jquery']
};


