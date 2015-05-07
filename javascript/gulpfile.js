'use strict';

var fs = require('fs');
var gulp = require('gulp');
var gutil = require('gulp-util');
var browserify = require('browserify');
var reactify = require('reactify');
var watchify = require('watchify');
var react = require('gulp-react');
var uglify = require('gulp-uglify');
var rename = require('gulp-rename');
var rm = require('gulp-rimraf');
var rimraf = require('rimraf');
var rev = require('gulp-rev');
var handlebars = require('gulp-compile-handlebars');
var runSequence = require('run-sequence');
var jshint = require('gulp-jshint');
var source = require('vinyl-source-stream');
var debug = require('gulp-debug');
var livereload = require('gulp-livereload');
var config = require('./build.properties');

var handlebarOpts = {
    helpers: {
        assetPath: function (path, context) {
            return [config.assetDir, context.data.root[path]].join('/');
        }
    }
};

var reactOpts = {
    harmony: true,
    es6: true,
    target: 'es5',
    stripTypes: true
};

var replaceRev = function (debug, noSync) {
    var manifest;
    if (!debug) {
        manifest = JSON.parse(fs.readFileSync('dist/rev-manifest.json', 'utf8'));
    } else {
        manifest = {
            "app.js":"app.js",
            livereload: !noSync && '<script src="//localhost:35729/livereload.js"></script>'
        };
    }

    return gulp.src(config.scriptTemplatePath)
        .pipe(handlebars(manifest, handlebarOpts))
        .pipe(rename(config.scriptDestinationName))
        .pipe(config.targetDir ? gulp.dest(config.targetDir) : gutil.noop())
        .pipe(gulp.dest(config.deployDir));
};
gulp.task('replace-rev-dev', function() {
    return replaceRev(true);
});
gulp.task('replace-rev-dev-no-sync', function() {
    return replaceRev(true, true);
});
gulp.task('replace-rev-prod', function() {
    return replaceRev(false);
});

gulp.task('clean', function () {
    return gulp.src('dist/*')
        .pipe(rm());
});

gulp.task('clean-target', function (callback) {
    rimraf(config.deployDirJs, callback);
});

gulp.task('prepare-lint', ['clean'], function () {
    return gulp.src('src/**/*.jsx')
        .pipe(react(reactOpts))
        .on('error', function (err) {
            gutil.log(err);
            this.emit('end');
        })
        .pipe(gulp.dest('dist/jsx'));
});

var lintSrc = ['src/**/*.js', 'dist/jsx/**/*.js', '!src/**/__tests__/**'];

gulp.task('lint', ['prepare-lint'], function () {
    return gulp.src(lintSrc)
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish', {verbose: true}));
});

gulp.task('lint-strict', ['prepare-lint'], function () {
    return gulp.src(lintSrc)
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish', {verbose: true}))
        .pipe(jshint.reporter('fail'));
});

function browserifyCall(debug) {
    var browserifyConfig = {
        entries: config.entryPoints,
        extensions: ['.jsx', '.js'],
        "transform": [
            ["reactify", reactOpts]
        ],
        debug: debug
    };
    if (debug) {
        browserifyConfig.cache = {};
        browserifyConfig.packageCache = {};
        browserifyConfig.fullPaths = true;
    }
    var b = browserify(browserifyConfig);
    b.plugin('tsify', {noImplicitAny: false, target: 'ES5'}).on('error', function (err) {
        gutil.log(err);
        this.emit('end');
    });

    config.browserifyExcludes && config.browserifyExcludes.forEach(function(exclude) {
        b.exclude(exclude);
    });
    return b;
}

function browserifyIt(debug) {
    return browserifyCall(debug)
        .bundle()
        .on('error', function (err) {
            gutil.log(err);
            this.emit('end');
        })
        .pipe(source('app.js'))
        .pipe(gulp.dest('./dist/'));
}

gulp.task('bundle-tests', function () {
    var b = browserify({
        entries: config.testEntryPoints,
        extensions: ['.jsx', '.js'],
        "transform": [
            ["reactify", reactOpts]
        ],
        debug: true
    });
    b.plugin('tsify', {noImplicitAny: false}).on('error', function (err) {
        gutil.log(err);
        this.emit('end');
    });
    return b.bundle()
        .pipe(source('tests.js'))
        .pipe(gulp.dest('./dist/'));
});

gulp.task('prod-js', function () {
    return browserifyIt(false);
});

gulp.task('debug-js', function () {
    return browserifyIt(true);
});

gulp.task('compress', function () {
    return gulp.src('./dist/app.js')
        .pipe(uglify())
        .pipe(gulp.dest('./dist/'))
});

gulp.task('rev', function () {
    return gulp.src('./dist/app.js')
        .pipe(rev())
        .pipe(gulp.dest('./dist/'))
        .pipe(rev.manifest())
        .pipe(gulp.dest('./dist/'));
});

gulp.task('deploy-rev', function () {
    return gulp.src(['./dist/app-*.js'])
        .pipe(config.targetDirJs ? gulp.dest(config.targetDirJs) : gutil.noop())
        .pipe(gulp.dest(config.deployDirJs));
});

gulp.task('deploy-prod', function (callback) {
    runSequence('lint-strict', 'clean-target', 'clean', 'prod-js', 'compress', 'rev', 'replace-rev-prod', 'deploy-rev', callback);
});

gulp.task('prepare-dev', function (callback) {
    runSequence('lint', 'clean-target', 'clean', 'replace-rev-dev', callback);
});

gulp.task('default', ['deploy-prod']);

gulp.task('watch', ['prepare-dev'], function () {
    var bundler = watchify(browserifyCall(true), {delay: 50});

    bundler.on('update', rebundle);

    function rebundle() {
        gutil.log("Rebundling");
        return bundler.bundle()
            // log errors if they happen
            .on('error', gutil.log.bind(gutil, 'Browserify Error'))
            .pipe(source('app.js'))
            .pipe(gulp.dest('./dist'))
            .pipe(config.targetDirJs ? gulp.dest(config.targetDirJs) : gutil.noop())
            .pipe(gulp.dest(config.deployDirJs))
            .pipe(livereload());
    }

    return rebundle();
});
