#!/usr/bin/env bash

isql=""
virtuoso_host="127.0.0.1"
virtuoso_port="1111"
virtuoso_usr="dba"
virtuoso_pwb="dba"

virtuoso_address=${virtuoso_host}${virtuoso_port}

#Check wether virtuoso is running
running = ${$isql ${virtuoso_address} $virtuoso_usr $virtuoso_pwd}

#Ask user for confirmation


#Transform database
$isql ${virtuoso_address} $virtuoso_usr $virtuoso_pwd VERBOSE=ON prepare_table.sql versioned_sparql_to_sql_text.sql exec_versioned_sparql.sql

#Check if virtuoso is running
if [ "$(ping -q -c1 google.com)" ];then wget -mnd -q 1 $virtuoso_host ;fi