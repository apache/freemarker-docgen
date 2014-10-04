'use strict';

var path      = require('path');

var gulp      = require('gulp');
var less      = require('gulp-less');
var rename    = require('gulp-rename');
var minifyCss = require('gulp-minify-css');
var prefix    = require('gulp-autoprefixer');

var BASE_DIR = path.join(__dirname, 'src', 'main', 'org', 'freemarker', 'docgen');
var OUT_DIR = path.join(BASE_DIR, 'statics');

var paths = {
  less: path.join(__dirname, 'node_modules'),
  targetCss: path.join(OUT_DIR, 'css')
};

gulp.task('styles', function() {
  gulp.src(path.join(BASE_DIR, 'less', 'styles.less');)
    .pipe( less() )

    // rename and prefix
    .pipe( rename( { basename: "docgen" } ) )
    .pipe( prefix( { cascade: false } ) )
    .pipe( gulp.dest( OUT_DIR ) )

    // minify
    .pipe( rename( { suffix: ".min" } ) )
    .pipe( minifyCss() )
    .pipe( gulp.dest( OUT_DIR ) )
});

gulp.task('default', ['styles']);