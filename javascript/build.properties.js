module.exports = {
    deployDir: '../app/views/system/users/',
    deployDirJs: '../app/assets/javascripts/users',
    targetDir: null,
    targetDirJs: null,

    assetDir: 'javascripts/users',
    scriptTemplatePath: 'scripts.hbs',
    scriptDestinationName: 'scripts.scala.html',
    entryPoints: ['./src/mount.jsx'],

    test : {
        unmockedModulePathPatterns: [
            "<rootDir>/node_modules/react/",
            "<rootDir>/node_modules/joi/"
        ]
    }
};


