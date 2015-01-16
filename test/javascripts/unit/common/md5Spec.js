(function() {
    'use strict';

    describe('md5 encryption', function() {
        beforeEach(module('quizzoCommonApp'));

        it('should do nothing yet', inject(function($filter) {
            var md5Filter = $filter('md5'),
                result = md5Filter('foobar');
            expect(result).not.toBe('foobar');
            expect(result).toBeDefined();
        }));
    });

}());
