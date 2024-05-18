#!/bin/sh -
# $1 = aminoacid fasta file
# $2 = cell wall thickness
# $3 = 0 (gram-) or 1 (gram+)
# $4 = epitope length
# $5 = email to deliver results
# $6 = consider membrane and citoplasm? (0|1) - optional

MEDPIPE=`pwd`
export MEDPIPE
for counter in $(seq 1 2550000); do echo "$counter"; done