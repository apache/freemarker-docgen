'use strict';

var path = require('path');

var gulp = require('gulp');
var less = require('gulp-less');
var rename = require('gulp-rename');
var minifyCss = require('gulp-minify-css');
var prefix = require('gulp-autoprefixer');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');

var BASE_DIR = path.join(__dirname, 'src', 'main', 'org', 'freemarker', 'docgen');
var OUT_DIR = path.join(BASE_DIR, 'statics');

gulp.task('styles', function() {
  gulp.src(path.join(BASE_DIR, 'less', 'styles.less'))
    .pipe(less({ paths: path.join(__dirname, 'node_modules') }))

    // rename and prefix
    .pipe(rename({ basename: 'docgen' }))
    .pipe(prefix({ cascade: false }))
    .pipe(gulp.dest(OUT_DIR))

    // minify
    .pipe(rename({ suffix: '.min' }))
    .pipe(minifyCss({
      advanced: false,
      restructuring: false,
      aggressiveMerging: false
    }))
    .pipe(gulp.dest(OUT_DIR));
});

gulp.task('js', function() {
  return gulp.src([
      path.join(BASE_DIR, 'statics', 'use-strict.js'),
      path.join(BASE_DIR, 'statics', 'make-toc.js'),
      path.join(BASE_DIR, 'statics', 'page-menu.js')
    ])
    .pipe(concat('main.js'))
    .pipe(gulp.dest(OUT_DIR))
    .pipe(uglify())
    .pipe(rename({ suffix: '.min' }))
    .pipe(gulp.dest(OUT_DIR));
});

gulp.task('default', ['styles', 'js']);

gulp.task('watch-less', ['styles'], function() {
  // watch less files
  gulp.watch([path.join(BASE_DIR, 'less', '**', '*')], ['styles']);
});
