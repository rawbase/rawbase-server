#!/usr/bin/env bash

#############################
#
# Install script for R&Wbase 0.1
#
############################
RAWBASE_HOME="."

isql=""
virtuoso_host="127.0.0.1"
virtuoso_port="1111"
virtuoso_usr="dba"
virtuoso_pwb="dba"

virtuoso_address=${virtuoso_host}${virtuoso_port}

#Check wether virtuoso is running
running = $($isql ${virtuoso_address} $virtuoso_usr $virtuoso_pwd)

if [[ $running == *"S2801"* ]]
then
    echo "[R&Wbase Install] Virtuoso 6.X is not running on address ${virtuoso_address}"
    exit 1
fi

#Ask user for confirmation
read -p "[R&Wbase Install] About to prepare Virtuoso 6.x for R&Wbase. This clears all present triples. Are you sure? [Y/N]" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    #Transform database
    echo "[R&Wbase Install] Transforming Virtuoso."
    $isql ${virtuoso_address} $virtuoso_usr $virtuoso_pwd VERBOSE=ON prepare_table.sql versioned_sparql_to_sql_text.sql exec_versioned_sparql.sql

    #build the maven project
    echo "[R&Wbase Install] Building Maven project."
    cd $RAWBASE_HOME
    mvn package

    #finished
    echo "[R&Wbase Install] Done! Now ready to run."
else
    echo "[R&Wbase Install] Aborted. R&Wbase was not installed."
fi