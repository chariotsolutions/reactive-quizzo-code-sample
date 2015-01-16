(function() {
    'use strict';

    angular.module('quizzoCommonApp', ['talis.filters.md5'])
    .constant('REST_API_PREFIX', '/api/v1.0/')
    .factory('authInterceptor', function($log, $q, credentials) {
        return  {
            responseError: function(response) {
                if (response.status === 401) {
                    $log.debug('forcing an authentication event');
                    credentials.authRequired = true;
                }
                // some other error outside of this domain,
                // forward along
                // if you forget to return this, your $http
                // calls will always return success, even with
                // no payload!
                return $q.reject(response);
            }
        };
    })
    .config(function($httpProvider) {
        $httpProvider.interceptors
            .push('authInterceptor');
        // enable cookie support
        // for session management
        // TODO - investigate tokens instead
        $httpProvider.defaults.withCredentials = true;
    });

}());
