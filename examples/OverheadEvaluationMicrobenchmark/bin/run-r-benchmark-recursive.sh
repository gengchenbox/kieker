#!/bin/bash

BINDIR=$(dirname $0)/
BASEDIR=${BINDIR}../

RESULTSDIR="${BASEDIR}tmp/results-benchmark-recursive/"
RESULTSFN="${RESULTSDIR}results.csv"

## Generate Results file
R --vanilla --silent <<EOF
results_fn="${RESULTSFN}"
output_fn="${RESULTSDIR}results.pdf"
source("${BINDIR}r-scripts/benchmark-recursive.r")
EOF
echo "results written to '${RESULTSDIR}results.pdf'"