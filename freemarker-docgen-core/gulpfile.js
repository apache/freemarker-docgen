'use strict';

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

var path = require('path');
var fs = require('fs');

var gulp = require('gulp');
var less = require('gulp-less');
var rename = require('gulp-rename');
var cleanCss = require('gulp-clean-css');
var prefix = require('gulp-autoprefixer');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');
var headerfooter = require('gulp-headerfooter');

var BASE_DIR = path.join(__dirname, 'src', 'main', 'resources-gulp', 'org', 'freemarker', 'docgen', 'core');
var OUT_DIR = path.join(__dirname, 'target', 'resources-gulp', 'org', 'freemarker', 'docgen', 'core', 'statics');

var copyrightHeader = fs.readFileSync(path.join(__dirname, 'gulp-output-copyright-header.txt'));
var doNotEditHeader = "/*\n"
        + " * GENERATED WITH GULP - DO NOT EDIT!\n"
        + " * Any copyright headers below are coming from the source files from which this file was generated.\n"
        + " * <#DO_NOT_UPDATE_COPYRIGHT>\n"
        + " */\n\n"

gulp.task('styles', gulp.series(function(done) {
  gulp.src(path.join(BASE_DIR, 'less', 'styles.less'))
    .pipe(less({ paths: path.join(__dirname, 'node_modules') }))

    // rename and prefix
    .pipe(rename({ basename: 'docgen' }))
    .pipe(prefix({ cascade: false }))
    .pipe(headerfooter.header(doNotEditHeader))
    .pipe(gulp.dest(OUT_DIR))

    // minify
    .pipe(rename({ suffix: '.min' }))
    .pipe(cleanCss({
      advanced: false,
      restructuring: false,
      aggressiveMerging: false
    }))
    .pipe(headerfooter.header(copyrightHeader))
    .pipe(gulp.dest(OUT_DIR));
	done();
}));

gulp.task('js', gulp.series(function(done) {
  return gulp.src([
      path.join(BASE_DIR, 'js', 'use-strict.js'),
      path.join(BASE_DIR, 'js', 'make-toc.js'),
      path.join(BASE_DIR, 'js', 'page-menu.js'),
      path.join(BASE_DIR, 'js', 'search.js')
    ])
    .pipe(concat('main.js'))
    .pipe(headerfooter.header(doNotEditHeader))
    .pipe(gulp.dest(OUT_DIR))
    .pipe(uglify())
    .pipe(rename({ suffix: '.min' }))
    .pipe(headerfooter.header(copyrightHeader))
    .pipe(gulp.dest(OUT_DIR));
	done();
}));

gulp.task('default', gulp.series(['styles', 'js']));

gulp.task('watch-less', gulp.series(['styles'], function(done) {
  // watch less files
  gulp.watch([path.join(BASE_DIR, 'less', '**', '*')], ['styles']);
  done();
}));
