/*
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
'use strict';

angular.module('activitiModeler')
    .controller('FormBuilderController', ['$rootScope', '$scope', '$translate', '$http', '$timeout', '$location', '$modal', '$routeParams', '$popover', 'FormBuilderService',
        function ($rootScope, $scope, $translate, $http, $timeout, $location, $modal, $routeParams, $popover, FormBuilderService) {

            // Main page (needed for visual indicator of current page)
            $rootScope.setMainPageById('forms');

            // Needs to be on root scope to be available in the toolbar controller
            $rootScope.formBuilder = {activeTab: 'design'};

            $scope.model = {
                useOutcomes: false
            };

            $rootScope.currentReadonlyFields = {fields: {}};

            $scope.$watch('model.useOutcomes', function (value) {
                if (value) {
                    if (!$rootScope.currentOutcomes || $rootScope.currentOutcomes.length === 0) {
                        $rootScope.currentOutcomes = [{name: ''}];
                    }
                } else {
                    $rootScope.currentOutcomes = [];
                }
            });

            // tabs for tab-control. NOT using templates for the tabs, we control the view
            // ourselves, based on the active tab binding
            $scope.tabs = [
                {
                    id: 'design',
                    title: 'FORM-BUILDER.TITLE.DESIGN'
                },
                {
                    id: 'outcome',
                    title: 'FORM-BUILDER.TITLE.OUTCOME'
                }
            ];

            $scope.form = {name: '', description: '', version: 1};

            $scope.formElements = [];
            $rootScope.currentOutcomes = [];

            $rootScope.formChanges = false;

            var guidSequence = 0;

            function setFieldDragDropAttributes (field, prefix) {
                if (!field._guid) {
                    field._guid = prefix + guidSequence++;
                }

                if (!field._width) {
                    field._width = 1;
                }
            }

            var lastDropArrayTarget = null;

            $scope.onFieldMoved = function (field, fieldArraySource) {
            };


            $scope.onFieldDrop = function (paletteElementOrField, dropArrayTarget, event, index) {

                // Is it an existing object?
                if (paletteElementOrField.hasOwnProperty('_guid')) {

                    lastDropArrayTarget = dropArrayTarget;

                    if (dropArrayTarget) {
                        var i = -1;
                        dropArrayTarget.forEach(function (f, index) {
                            if (paletteElementOrField._guid == f._guid) {
                                i = index;
                            }
                        });
                        if (i != -1) {
                            dropArrayTarget.splice(i, 1);
                        }
                    }

                    if (navigator.appVersion.indexOf("MSIE 9") != -1) {
                        // update _guid which is what ngRepeat is tracking by to force dom updates
                        paletteElementOrField._guid += '_';
                    }
                    return paletteElementOrField;
                }

                lastDropArrayTarget = null;

                var fieldId = paletteElementOrField.type;
                var fieldType;

                var colspan = 1;
                var numberOfRows = 1;

                var field = {
                    type: fieldId,
                    fieldType: fieldType,
                    name: 'Label',
                    required: false,
                    readOnly: false,
                    sizeX: colspan,
                    sizeY: numberOfRows,
                    row: -1,
                    col: -1
                };
                setFieldDragDropAttributes(field, 'newField');

                if (fieldId === 'radio-buttons') {
                    field.options = [
                        {name: $translate.instant('FORM-BUILDER.COMPONENT.RADIO-BUTTON-DEFAULT')}
                    ];
                } else if (fieldId === 'dropdown') {
                    field.options = [
                        {name: $translate.instant('FORM-BUILDER.COMPONENT.DROPDOWN-DEFAULT-EMPTY-SELECTION')}
                    ];
                    field.value = field.options[0];
                    field.hasEmptyValue = true;
                } else if (fieldId === 'formatted-text') {
                    field.fieldType = 'FormattedTextFieldRepresentation';
                }

                if (field.type === 'readonly-text') {
                    field.value = $translate.instant('FORM-BUILDER.COMPONENT.DISPLAY-TEXT-DEFAULT');
                }


                return field;
            };


            if ($routeParams.modelId) {

                var url;
                if ($routeParams.modelHistoryId) {
                    url = ACTIVITI.CONFIG.contextRoot + '/app/rest/form-models/' + $routeParams.modelId
                    + '/history/' + $routeParams.modelHistoryId;
                } else {
                    url = ACTIVITI.CONFIG.contextRoot + '/app/rest/form-models/' + $routeParams.modelId;
                }

                $http({method: 'GET', url: url}).
                    success(function (response, status, headers, config) {
                        if (response.formDefinition.fields) {

                            for (var i = 0; i < response.formDefinition.fields.length; i++) {
                                var field = response.formDefinition.fields[i];
                                if (!field.params) {
                                    field.params = {};
                                }
                                setFieldDragDropAttributes(field, 'savedField');
                            }

                            $scope.formElements = response.formDefinition.fields;
                        } else {
                            $scope.formElements = [];
                        }
                        if (response.formDefinition.outcomes) {
                            $rootScope.currentOutcomes = response.formDefinition.outcomes;
                            if ($rootScope.currentOutcomes.length > 0) {
                                $scope.model.useOutcomes = true;
                            }
                        } else {
                            $rootScope.currentOutcomes = [];
                        }
                        $rootScope.currentForm = response;
                        delete $rootScope.currentForm.formDefinition;

                        $rootScope.formItems = $scope.formElements;

                        $rootScope.formChanges = false;
                        $timeout(function () {
                            // Flip switch in timeout to start watching all form-related models
                            // after next digest cycle, to prevent first false-positive
                            $scope.formLoaded = true;
                        }, 200);
                    }).
                    error(function (response, status, headers, config) {
                        $scope.model.loading = false;
                    });
            } else {
                $scope.formLoaded = true;
            }

            $scope.palletteElements = [
                {'type': 'text', 'title': $translate.instant('FORM-BUILDER.PALLETTE.TEXT'), 'icon': 'images/form-builder/textfield-icon.png', 'width': 1},
                {'type': 'formatted-text', 'title': $translate.instant('FORM-BUILDER.PALLETTE.FORMATTED-TEXT'), 'icon': 'images/form-builder/formatted-textfield-icon.png', 'width': 1},
                {'type': 'multi-line-text', 'title': $translate.instant('FORM-BUILDER.PALLETTE.MULTILINE-TEXT'), 'icon': 'images/form-builder/multi-line-textfield-icon.png', 'width': 1},
                {'type': 'integer', 'title': $translate.instant('FORM-BUILDER.PALLETTE.NUMBER'), 'icon': 'images/form-builder/numberfield-icon.png', 'width': 1},
                {'type': 'boolean', 'title': $translate.instant('FORM-BUILDER.PALLETTE.CHECKBOX'), 'icon': 'images/form-builder/booleanfield-icon.png', 'width': 1},
                {'type': 'date', 'title': $translate.instant('FORM-BUILDER.PALLETTE.DATE'), 'icon': 'images/form-builder/datefield-icon.png', 'width': 1},
                {'type': 'dropdown', 'title': $translate.instant('FORM-BUILDER.PALLETTE.DROPDOWN'), 'icon': 'images/form-builder/dropdownfield-icon.png', 'width': 1},
                {'type': 'radio-buttons', 'title': $translate.instant('FORM-BUILDER.PALLETTE.RADIO'), 'icon': 'images/form-builder/choicefield-icon.png', 'width': 1},
                {'type': 'people', 'title': $translate.instant('FORM-BUILDER.PALLETTE.PEOPLE'), 'icon': 'images/form-builder/peoplefield-icon.png', 'width': 1},
                {'type': 'functional-group', 'title': $translate.instant('FORM-BUILDER.PALLETTE.GROUP-OF-PEOPLE'), 'icon': 'images/form-builder/peoplefield-icon.png', 'width': 1},
                {'type': 'upload', 'title': $translate.instant('FORM-BUILDER.PALLETTE.UPLOAD'), 'icon': 'images/form-builder/uploadfield-icon.png', 'width': 1},
                {'type': 'readonly', 'title': $translate.instant('FORM-BUILDER.PALLETTE.DISPLAY-VALUE'), 'icon': 'images/form-builder/readonly-icon.png', 'width': 1},
                {'type': 'readonly-text', 'title': $translate.instant('FORM-BUILDER.PALLETTE.DISPLAY-TEXT'), 'icon': 'images/form-builder/readonly-text-icon.png', 'width': 1}
            ];

            $scope.$watch('formItems', function () {
                if ($scope.formLoaded) {
                    $rootScope.formChanges = true;
                }
            }, true);

            $scope.$watch('currentOutcomes', function () {
                if ($scope.formLoaded) {
                    $rootScope.formChanges = true;
                }
            }, true);

            $scope.addOutcome = function () {
                $rootScope.currentOutcomes[$rootScope.currentOutcomes.length] = {name: ''};
            };

            $scope.removeOutcome = function (index) {
                $rootScope.currentOutcomes.splice(index, 1);
            };

            $scope.$on('$locationChangeStart', function (event, next, current) {
                if (!$rootScope.ignoreChanges && $rootScope.formChanges) {
                    // Always prevent location from changing. We'll use a popup to determine the action we want to take
                    event.preventDefault();

                    var discardCallback = function () {
                        $rootScope.ignoreChanges = true;
                        $location.url(next.substring(next.indexOf('/#') + 2));
                    };

                    var continueEditingCallback = function () {
                        // Don't change the location and make sure "main navigation" is still correct
                        $rootScope.ignoreChanges = false;
                        $rootScope.setMainPageById('forms');
                    };

                    $scope.yesNoCancel = false;
                    showDiscardPopup($scope, null, discardCallback, continueEditingCallback);

                } else {
                    // Clear marker
                    $rootScope.ignoreChanges = false;
                }

            });

            $scope.$on("formChangesEvent", function () {
                var discardCallback = function () {
                    $scope.$broadcast("discardDataEvent");
                };

                var saveDataCallback = function () {
                    $scope.$broadcast("mustSaveEvent");
                };

                var continueEditingCallback = function () {
                    $scope.$broadcast("continueEditingEvent");
                };

                $scope.yesNoCancel = true;
                showDiscardPopup($scope, saveDataCallback, discardCallback, continueEditingCallback);
            });

            function showDiscardPopup($scope, saveCallback, discardCallback, cancelCallback) {
                if (!$scope.unsavedChangesModalInstance) {

                    $scope.handleResponseFunction = function (discard) {
                        $scope.unsavedChangesModalInstance = undefined;
                        if (discard == true) {
                            if (discardCallback) {
                                discardCallback();
                            }

                        } else if (discard == false) {
                            if (saveCallback) {
                                saveCallback();
                            }
                        } else {
                            if (cancelCallback) {
                                cancelCallback();
                            }
                        }
                    };

                    _internalCreateModal({
                        template: 'editor-app/popups/unsaved-changes.html',
                        scope: $scope
                    }, $modal, $scope);
                }
            };

            // get current step and sequential steps for form field resolving
            if ($rootScope.editorHistory && $rootScope.editorHistory.length > 0) {
                $scope.stepId = $rootScope.editorHistory[0].stepId;
                $scope.allSteps = $rootScope.editorHistory[0].allSteps;
            }

        }]);
