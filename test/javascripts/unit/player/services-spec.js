(function() {
  'use strict';

  describe('gamePlayerService', function() {
    var $httpBackend, gamePlayerService, $log, $rootScope, REST_API_PREFIX;

    beforeEach(module('quizzoPlayerApp'));

    beforeEach(inject(function(_$httpBackend_, _gamePlayerService_, _$log_, _$rootScope_, _REST_API_PREFIX_) {
      $httpBackend = _$httpBackend_;
      gamePlayerService = _gamePlayerService_;
      $log = _$log_;
      $rootScope = _$rootScope_;
      REST_API_PREFIX = _REST_API_PREFIX_;
    }));

    it('should list games', function() {
      var result = JSON.stringify([
        { title: 'Javascript Quiz', description: 'This is the Javascript Quiz', id: 'jsquiz' },
        { title: 'Visual Basic Quiz', description: 'This is the Visual Basic Quiz', id: 'lamequiz' } ]);
      $httpBackend.expectGET(REST_API_PREFIX + 'player/games').respond(200, result);
        var games = null,
            promise = gamePlayerService.getGames();

        promise.then(
          function(response) {
            games = response;
          }
        );

        $httpBackend.flush();

        waitsFor(function() {
          return games !== null;
        }, 'call to complete.', 3000);

        runs(function() {

          expect(games).toBeDefined();
          expect(games[0].title).toBe('Javascript Quiz');
          expect(games[1].title).toBe('Visual Basic Quiz');
          expect(games.length).toBe(2);
          $httpBackend.verifyNoOutstandingExpectation();
        });

    });

    it('should join a game', inject(function(gamePlayerService) {
        var response = null,
            promise,
            expectedPostBody = JSON.stringify(
                { command: 'join', nickname: 'joey', email: 'joey@oncheers.com'});

      $httpBackend.expectPOST(REST_API_PREFIX + 'player/games/jsquiz',
          expectedPostBody).respond(200, { id: 'raw'});
        promise = gamePlayerService.doJoinGameInstance('jsquiz', 'joey', 'joey@oncheers.com'),
        promise.then(
          function(httpResponse) {
            response = httpResponse;
          }
        );

        $httpBackend.flush();

//        waitsFor(function() {
//          return response !== null;
//        }, 'call to complete.', 3000);

//        runs(function() {
          expect(response).toBe(true);
          $httpBackend.verifyNoOutstandingExpectation();
//        });
    }));

    it('should answer a question', inject(function(gamePlayerService) {
        var response = null,
            promise = gamePlayerService.answerQuestion('A'),
            expectedPostBody = JSON.stringify({ command: 'answerQuestion', answer: 'A'});

      $httpBackend.expectPOST(REST_API_PREFIX + 'player/games/jsquiz', expectedPostBody).respond(200, 'OK');
        promise.then(
          function(httpResponse) {
            response = httpResponse;
          }
        );
 
        $httpBackend.flush();

        waitsFor(function() {
          return response !== null;
        }, 'call to complete.', 3000);

        runs(function() {
          expect(response).toBe(200);
          $httpBackend.verifyNoOutstandingExpectation();
        });
    })); 

  });
}());
