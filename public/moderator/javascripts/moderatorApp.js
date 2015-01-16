(function() {
  'use strict';

  angular.module('quizzoModeratorApp', ['ngRoute', 'ngAnimate', 'quizzoCommonApp'])
    .config(function($routeProvider) {
          $routeProvider
              .when('/login', {
                  templateUrl: 'templates/login.html',
                  controller: 'ModeratorSignonCtrl'
              })
              .when('/moderate', {
                  templateUrl: 'templates/moderate.html',
                  controller: 'ModeratorCtrl'
              })
              .when('/logout', {
                  templateUrl: 'SignoutCtrl',
                  controller: 'templates/logout.html'

              })
              .otherwise({
                  redirectTo: '/login'
              });
    })
    .run(function($rootScope, credentialsService) {
        // expose credentials on root scope.  Must. Be. Another. Way. But. No. MessageBus.
        $rootScope.credentials = credentialsService.getCredentials();
    });

}());
