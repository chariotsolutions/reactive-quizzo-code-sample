(function() {
    'use strict';
    angular.module('quizzoModeratorApp').factory('moderator', function() {
        return {
            authenticated: false,
            userId: null,
            // todo - refactor state of games into this area
            runningGames: [],
            availableGames: []
        };
    });
}());