(function () {
    'use strict';
    angular.module('quizzoCommonApp')
        .value('credentials', {
            authRequired: true,
            roles: [],
            userId: null,
            session: null,
            isModerator : function() {
                var results = this.roles.filter(function(role) {
                    return role.name === 'moderator'
                });
                return results.length === 1;
            }
        })
        .factory('credentialsService', function ($log, $filter, $http, $q, REST_API_PREFIX, credentials) {
            var md5Filter = $filter('md5'),
                service = {};
            service.setAuthRequired = function () {
                credentials.authRequired = true;
            };
            service.clearAuthRequired = function () {
                credentials.authRequired = false;
            };
            service.isAuthRequired = function () {
                return credentials.authRequired;
            };

            // this feels sooo wrong
            service.getCredentials = function () {
                return credentials;
            };

            return service;
        })

        .factory('commonServices', function ($log, $filter, $http, $q, REST_API_PREFIX, credentials) {
            var service = {};
            service.getGames = function (playerType) {            // precondition
                var deferred = $q.defer(),
                    errorFn = function (response) {
                        $log.error('game list function failed.', response.status, response.data);
                        deferred.reject('game list failed. reason ' + response.data);
                    },
                    successFn = function (response) {
                        deferred.resolve(response.data);
                    };

                $http.get(REST_API_PREFIX + playerType + '/games', {}

                ).then(successFn, errorFn);

                return deferred.promise;
            };
            return service;
        });
}());

