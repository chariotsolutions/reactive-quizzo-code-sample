(function() {
    'use strict';

    describe('moderator services', function() {

        var moderatorService, moderator, $httpBackend;

        beforeEach(module('quizzoModeratorApp'));

        beforeEach(inject(function(_moderatorService_, _$httpBackend_) {
            moderatorService = _moderatorService_;
            $httpBackend = _$httpBackend_;
        }));

        it('should signon with good login', inject(function(moderator) {
            var response = {
                id: '23lk4j234234',
                userId: 'ken',
                roles: [
                    { name: 'moderator'}
                ]
            };
            $httpBackend.expectPOST('/login', {
                user: 'ken',
                password: 'f632fa6f8c3d5f551c5df867588381ab'
            }).respond(200, response);

            var promise = moderatorService.signon('ken', 'ken'),
                promiseResult = false;

            promise.then(
                function(result) {
                    promiseResult = true;
                });

            $httpBackend.flush();
            expect(promiseResult).toBe(true);
            expect(moderator.userId).toBe('ken');
            expect(moderator.authenticated).toBe(true);

        }));

        it('should fail with invalid login', inject(function(moderator) {
            console.log(moderator);
            var response = {
                id: '23lk4j234234',
                userId: 'ken',
                roles: [
                    { name: 'moderator'}
                ]
            };
            $httpBackend.expectPOST('/login', {
                user: 'ken',
                password: 'f632fa6f8c3d5f551c5df867588381ab'
            }).respond(401, "Unauthorized");

            var promise = moderatorService.signon('ken', 'ken'),
                promiseResult = false;
            promise.then(
                function(result) {
                    promiseResult = true;
                });

            $httpBackend.flush();
            expect(promiseResult).toBe(false);
            expect(moderator.userId).toBeNull();
            expect(moderator.authenticated).toBe(false);

        }));


        it('should start a game when requested', function() {
            $httpBackend.expectPOST('/api/v1.0/moderator/games/Foo', { command: 'start'}).respond(200, 'OK');
            var expectation = false;
            moderatorService.startGame('Foo')
                .then(function (success) {
                    expectation = true;
                });


            $httpBackend.flush();
            expect(expectation).toBe(true);
        });

            it('should ask for another question requested', function() {
            $httpBackend.expectPOST('/api/v1.0/moderator/games/Foo', { command: 'nextQuestion'}).respond(200);
            moderatorService.nextQuestion('Foo')
                .then(function(success) {

                }, function(error) {
                    expect(1).toBe(2);
                });
            $httpBackend.flush();

        });

    });
}());