(function () {
    'use strict';


    angular.module('quizzoModeratorApp')
        .service('moderatorService',
        function (REST_API_PREFIX, $http, $q, $filter, $log, moderator) {
            var sendCommand = function (gameId, command) {
                var deferred = $q.defer();
                $http.post(REST_API_PREFIX + 'moderator/games/' + gameId,
                           { command: command },
                           { headers: { session: moderator.session}})
                    .success(function (response) {
                        deferred.resolve(true);
                    })
                    .error(function (error) {
                        moderator.currentGameId = null;
                        $log.error('cannot start game', gameId, 'reason', error.status, error.data);
                        deferred.reject(error.status);
                    });
                return deferred.promise;
            };

            var md5Filter = $filter('md5');
            return {

                signon: function (userId, password) {
                    var deferred = $q.defer(),
                        md5Pass = md5Filter(password),
                        successFn, errorFn;

                    successFn = function (success) {
                        // TODO - yuck!
                        if (success.data.id !== null) {
                            moderator.session = success.data.id;
                        } else {
                            deferred.reject('No session ID returned');
                        }

                        if (success.data.userId !== undefined) {
                            moderator.userId = success.data.userId;
                        } else {
                            deferred.reject('No returned username, cannot continue');
                            return;
                        }
                        // we're in, until we get another intercepted 401
                        moderator.authenticated = true;
                        deferred.resolve('success!');
                    };
                    errorFn = function (error) {
                        $log.error('Cannot sign in, reason follows.', error);
                        if (error.status >= 500) {
                            $log.error(error);
                            deferred.reject('Server error.');
                        } else if (error.status >= 400) {
                            deferred.reject('Not Authorized.');
                        } else {
                            $log.error('Unrecognized error!', error);
                            deferred.reject('Unrecognized error.');
                        }
                        // in cases other than unauth, log it
                        moderator.authorized = false;
                        moderator.userId = null;
                        // log our error
                        $log.error('Cannot sign in, reason follows.', error);
                    };
                    // now, do the call and handle the results
                    $http.post('/login', { user: userId, password: md5Pass })
                        .then(successFn, errorFn);
                    return deferred.promise;
                },

                getRunningGameInstances: function () {            // precondition
                    var deferred = $q.defer(),
                        errorFn = function (response) {
                            $log.error('game list function failed.', response.status, response.data);
                            deferred.reject('game list failed. reason ' + response.data);
                        },
                        successFn = function (response) {
                            deferred.resolve(response.data);
                        };

                    $http.get(REST_API_PREFIX + 'moderator/games',
                        { headers: {
                            session: moderator.session }
                        })
                        .then(successFn, errorFn);

                    return deferred.promise;
                },

                getGameDefinitions: function () {
                    var deferred = $q.defer(),
                        errorFn = function (response) {
                            $log.error('game list function failed.', response.status, response.data);
                            deferred.reject('game list failed. reason ' + response.data);
                        },
                        successFn = function (response) {
                            deferred.resolve(response.data);
                        };


                    $http.get(REST_API_PREFIX + 'moderator/available-games',
                        { headers: {
                            session: moderator.session }
                        })
                        .then(successFn, errorFn);

                    return deferred.promise;
                },

                startGame: function(gameId) {
                    return sendCommand(gameId, 'start');
                },

                advance: function (gameId) {
                    return sendCommand(gameId, 'nextQuestion');
                }

            };
        });
}());
