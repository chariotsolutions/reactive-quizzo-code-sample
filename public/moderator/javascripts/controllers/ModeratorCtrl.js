(function () {
    'use strict';
    angular.module('quizzoModeratorApp')
        .controller('ModeratorCtrl', function ($scope, moderatorService, moderator) {

            // todo - does this belong in the moderator service?
            $scope.moderator = moderator;

            $scope.getGameDefinitions = function () {

                moderatorService.getGameDefinitions().then(
                    function (games) {
                        $scope.gameDefinitions = games
                    },
                    function (error) {
                        $scope.gameDefinitions = null;
                    }
                );
            };



            $scope.getRunningGames = function () {

                moderatorService.getRunningGameInstances().then(
                    function (games) {
                        $scope.runningGameInstances = games
                    },
                    function (error) {
                        $scope.runningGameInstances = null;
                    }
                );
            };

            $scope.startGame = function(gameId) {
                moderatorService.startGame(gameId)
                    .then(function() {
                        // refresh the running game instances once we start a new one
                        $scope.getRunningGames();
                    });
            };

            $scope.advance = function (gameInstanceId) {
                moderatorService.advance(gameInstanceId);
            };

            // load both option lists
            $scope.getGameDefinitions();
            $scope.getRunningGames();
        });
}());
