(function() {
    'use strict';

    var gulp = require('gulp'),
        jshint = require('gulp-jshint'),
        karma = require('karma').server,
        bowerDir = 'public/bower_components',
        publicJSDir = 'public/javascripts/applications/',
        librariesPath = [
            bowerDir + '/angular/angular.js',
            bowerDir + '/angular-route/angular-route.js',
            bowerDir + '/md5-angular-filter/js/md5.filter.js'
        ],
        testLibrariesPath = [
            bowerDir + '/angular-mocks/angular-mocks.js'
        ],
        sourcePath = [
            publicJSDir + 'common/quizzoCommonApp.js',
            publicJSDir + 'player/playerApp.js',
            publicJSDir + 'moderator/moderatorApp.js',
            publicJSDir + '**/services/**/*.js',
            publicJSDir + '**/controllers/**/*.js',
            publicJSDir + '**/filters/**/*.js'
        ],
        testPath = [
            'test/javascripts/**/*.js'
        ],
        testSrc = gulp.src(librariesPath.concat(librariesPath, sourcePath, testLibrariesPath, testPath));

    gulp.task('hint-sources', function() {
        gulp.src(sourcePath)
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish'))
        .pipe(jshint.reporter('fail'));
    });

    gulp.task('hint-test-sources', function() {
        gulp.src(testPath)
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish'))
        .pipe(jshint.reporter('fail'));
    });

    gulp.task('hint', ['hint-sources', 'hint-test-sources']);

    gulp.task('test', function() {
        return testSrc
            .pipe(karma({
                configFile: 'karma.conf.js',
                action: 'run'
            }))
            .on('error', function(err) {
                throw err;
            });
    });

}());
