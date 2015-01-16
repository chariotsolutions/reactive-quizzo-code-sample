(function() {
    'use strict';

    describe('Moderator Controller', function() {
        var $scope, $httpBackend, moderator;

        beforeEach(module('quizzoModeratorApp'));

        beforeEach(inject(function($rootScope, $controller, _$httpBackend_, _moderator_) {
            $scope = $rootScope.$new();
            $httpBackend = _$httpBackend_;
            moderator = _moderator_;
            $controller('ModeratorCtrl', {
                $scope: $scope
            });
            $httpBackend.whenGET('/api/v1.0/moderator/available-games').respond(200, testGames);

        }));

        it('should fetch a list of available games', function() {
            $httpBackend.expectGET('/api/v1.0/moderator/available-games').respond(200, testGames);
            $scope.getGameDefinitions();
            $httpBackend.flush();
            expect($scope.availableGames.length).toBe(2);
        });

        it('should fetch a list of running games', function() {
            //$httpBackend.whenGET('/api/v1.0/moderator/available-games').respond(200, testGames);
            $httpBackend.expectGET('/api/v1.0/moderator/games').respond(200, testGames);
            $scope.getRunningGames();
            $httpBackend.flush();
            expect($scope.runningGames.length).toBe(2);
        });

        it('should start a game', function() {
            $httpBackend.expectPOST('/api/v1.0/moderator/games/FOO', { command: 'start'}).respond(200);
            $scope.moderator.currentGameId = 'FOO';
            $scope.startGame();
            $httpBackend.flush();

        });

        afterEach(function() {
            $httpBackend.verifyNoOutstandingExpectation();
            $httpBackend.verifyNoOutstandingRequest();
        });
    });

    var testGames =  [
        {
            id: 'jsQuiz',
            title: 'Javascript Quiz',
            description: 'The language that keeps on giving. Heart Attacks.',
            questions: []
        },
        {
            id: 'vbQuiz',
            title: 'Visual Basic Quiz',
            description: 'The language that IS a heart attack',
            questions: []
        }
    ];
}());
