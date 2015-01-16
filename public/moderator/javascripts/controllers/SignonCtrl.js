(function () {
    'use strict';
    angular.module('quizzoModeratorApp')
        .controller('ModeratorSignonCtrl', function ($scope, $location, moderatorService) {
            $scope.authenticate = function (user) {
                $scope.message = 'Login in progress...';
                moderatorService.signon(user.name, user.password)
                    .then(
                    function (success) {
                        $scope.message = 'login successful';
                        $location.path('/moderate');
                    },
                    function (error) {
                        $scope.message = 'cannot login...';
                    });
            };
        });
}());
