#!/bin/bash
# Lists every method this branch adds, so a reviewer can judge each one instead of reading the whole diff.
# For each method ask: is it called? does one already exist? is it in the right layer? is it in the blueprint?
#
# Usage:  scripts/new-methods.sh [base]      base defaults to origin/main
# Run it from Git Bash (it comes with Git for Windows).

base="${1:-origin/main}"
export base

git diff -U0 "$base"...HEAD -- 'src/main/java/*.java' | awk '
  /^\+\+\+ b\// { file = substr($0, 7); next }
  # a class-level or nested-class member: 4 or 8 spaces, ends with an opening brace, has parentheses
  /^\+( {4}| {8})[A-Za-z<@].*\).*\{[ \t\r]*$/ {
    line = substr($0, 2)
    gsub(/^[ \t]+|[ \t\r]+$/, "", line)
    if (line ~ /^(if|for|while|switch|catch|else|try|do|return|new|synchronized|static \{)[ (]/) next
    if (line ~ /->/ || line ~ /^@/) next
    sub(/ *\{$/, "", line)
    print file "\t" line
  }
' | awk -F'\t' '
  { count[$1]++; rows[NR] = $0; order[NR] = $1 }
  END {
    last = ""
    for (i = 1; i <= NR; i++) {
      split(rows[i], p, "\t")
      if (p[1] != last) { printf "\n%s (%d)\n", p[1], count[p[1]]; last = p[1] }
      printf "  + %s\n", p[2]
    }
    if (NR == 0) print "No methods added compared with " ENVIRON["base"] "."
    else printf "\n%d methods added. Paste this list in the pull request and answer the five questions for each.\n", NR
  }
'
