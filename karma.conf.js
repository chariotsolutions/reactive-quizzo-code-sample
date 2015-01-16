// Karma configuration
// Generated on Mon Aug 11 2014 14:21:14 GMT-0400 (EDT)

module.exports = function(config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],


    // list of files / patterns to load in the browser
    files: [
      'public/bower_components/bower_components/jquery/dist/jquery.js',
      'public/bower_components/angular/angular.js',
      'public/bower_components/angular-cookies/angular-cookies.js',
      'public/bower_components/angular-route/angular-route.js',
      'public/bower_components/md5-angular-filter/js/md5.filter.js',
      'public/bower_components/angular-mocks/angular-mocks.js',
      'public/javascripts/applications/common/quizzoCommonApp.js',
      'public/javascripts/applications/player/playerApp.js',
      'public/javascripts/applications/moderator/moderatorApp.js',
      'public/javascripts/applications/**/services/**/*.js',
      'public/javascripts/applications/**/controllers/**/*.js',
      'public/javascripts/applications/**/filters/**/*.js',
      'public/app/js/**/*.js',
      'test/javascripts/mocks/**/*.js',
      'test/javascripts/unit/**/*.js'
    ],


    // list of files to exclude
    exclude: [
    ],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
    },


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['dots', 'progress', 'story'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_ERROR,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['Chrome'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false
  });
};
