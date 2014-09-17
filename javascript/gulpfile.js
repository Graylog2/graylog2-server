'use strict';

var fs = require('fs');
var gulp = require('gulp');
var gutil = require('gulp-util');
var browserify = require('browserify');
var reactify = require('reactify');
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

var deployDir = '../app/views/system/users/';
var deployDirJs = '../app/assets/javascripts/users';

var handlebarOpts = {
    helpers: {
        assetPath: function (path, context) {
            return ['javascripts/users', context.data.root[path]].join('/');
        }
    }
};

gulp.task('replace-rev', function () {
    var manifest = JSON.parse(fs.readFileSync('dist/rev-manifest.json', 'utf8'));

    return gulp.src('scripts.hbs')
        .pipe(handlebars(manifest, handlebarOpts))
        .pipe(rename('scripts.scala.html'))
        .pipe(gulp.dest(deployDir));
});

gulp.task('clean', function() {
    return gulp.src('dist/*')
        .pipe(rm());
});

gulp.task('clean-target', function(callback) {
    rimraf(deployDirJs, callback);
});

gulp.task('prepare-lint', ['clean'], function() {
    return gulp.src('src/**/*.jsx')
        .pipe(react({
            harmony: true
        }))
        .on('error', function(err) {
            gutil.log(err);
            this.emit('end');
        })
        .pipe(gulp.dest('dist/jsx'));
});

gulp.task('lint', ['prepare-lint'], function() {
    return gulp.src(['src/**/*.js', 'dist/jsx/**/*.js'])
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish', { verbose: true }));
});

gulp.task('lint-strict', ['prepare-lint'], function() {
    return gulp.src(['src/**/*.js', 'dist/jsx/**/*.js'])
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish', { verbose: true }))
        .pipe(jshint.reporter('fail'));
});

function browserifyIt(debug) {
    return browserify({
        entries: ['./src/mount.jsx'],
        extensions: ['.jsx', '.js'],
        "transform": [
            ["reactify", {
                harmony: true
            }]
        ],
        debug: debug
    })
        .bundle()
        .on('error', function(err) {
            gutil.log(err);
            this.emit('end');
        })
        .pipe(source('app.js'))
        .pipe(gulp.dest('./dist/'));
}

gulp.task('debug-js', function () {
    return browserifyIt(true);
});

gulp.task('prod-js', function() {
    return browserifyIt(false);
});

gulp.task('compress', function() {
    return gulp.src('./dist/app.js')
        .pipe(uglify())
        .pipe(gulp.dest('./dist/'))
});

gulp.task('rev', function() {
    return gulp.src('./dist/app.js')
        .pipe(rev())
        .pipe(gulp.dest('./dist/'))
        .pipe(rev.manifest())
        .pipe(gulp.dest('./dist/'));
});

gulp.task('rev-dummy', function() {
    fs.writeFileSync('dist/rev-manifest.json', JSON.stringify({"app.js": "app.js"}));
});

gulp.task('deploy-rev', function() {
    return gulp.src(['./dist/app-*.js'])
        .pipe(gulp.dest(deployDirJs));
});

gulp.task('deploy', function() {
    return gulp.src(['./dist/app.js'])
        .pipe(gulp.dest(deployDirJs));
});

gulp.task('deploy-prod', function(callback) {
    runSequence('lint-strict', 'clean-target', 'clean', 'prod-js', 'compress', 'rev', 'replace-rev', 'deploy-rev', callback);
});

gulp.task('deploy-dev', function(callback) {
    runSequence('lint', 'clean-target', 'clean', 'debug-js', 'rev-dummy', 'replace-rev', 'deploy', callback);
});

gulp.task('build-test', function(callback) {
    runSequence('clean', 'debug-js', callback);
});

gulp.task('watch', ['deploy-dev'], function() {
    gulp.watch('./src/**/*.*', ['deploy-dev']);
});

gulp.task('default', ['watch']);