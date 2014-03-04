#!/bin/bash

#############################
#
# Install script for R&Wbase 0.1
#
############################

isql="isql"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

virtuoso_address="127.0.0.1:1111"
virtuoso_usr="dba"
virtuoso_pwd="dba"


while getopts ":h:i:a:u:p:" opt; do
  case $opt in
    h)
      echo "[R&Wbase Install] RAWBASE_HOME is set to $OPTARG" >&2
      RAWBASE_HOME=$OPTARG
      ;;
    i)
      echo "[R&Wbase Install] Setting isql to $OPTARG" >&2
      isql=$OPTARG
      ;;
    a)
      echo "[R&Wbase Install] Looking for running at $OPTARG" >&2
      virtuoso_address=$OPTARG
      ;;
    u)
      virtuoso_usr=$OPTARG
      ;;
    p)
      virtuoso_pwd=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

if [ -z $RAWBASE_HOME ]
then
        echo "[R&Wbase Install] RAWBASE_HOME is not set. Please use"
        echo "[R&Wbase Install]         export RAWBASE_HOME=/path/to/rawbase-server"
        exit 1
else 
   if [ ! -e ${RAWBASE_HOME} ]
   then
       echo "[R&Wbase Install] RAWBASE_HOME is not a valid path."
       exit 1
   fi
fi


#Check wether virtuoso is running
running="$($isql $virtuoso_address $virtuoso_usr $virtuoso_pwd VERBOSE=OFF 'exec=status()')"

if [[ -z $running ]]
then
    echo "[R&Wbase Install] Virtuoso 6.X is not running on address ${virtuoso_address}. Install aborted."
    exit 1
fi

#Ask user for confirmation
read -p "[R&Wbase Install] About to prepare Virtuoso 6.x for R&Wbase. This clears all present triples. Are you sure? [Y/N]" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    #Transform database
    echo "[R&Wbase Install] Transforming Virtuoso."
    $isql ${virtuoso_address} $virtuoso_usr $virtuoso_pwd VERBOSE=ON $DIR/prepare_table.sql $DIR/versioned_sparql_to_sql_text.sql $DIR/exec_versioned_sparql.sql

    #build the maven project
    echo "[R&Wbase Install] Building Maven project."
    cd $RAWBASE_HOME

    mvn org.apache.maven.plugins:maven-install-plugin:2.5.1:install-file -Dfile=../src/main/resources/virtjdbc4_1.jar -DgroupId=openlink.org -DartifactId=virtjdbc -Dversion=4.1 -Dpackaging=jar
    mvn org.apache.maven.plugins:maven-install-plugin:2.5.1:install-file -Dfile=../src/main/resources/virt_jena2.jar -DgroupId=openlink.org -DartifactId=virt-jena -Dversion=2.0 -Dpackaging=jar

    mvn clean package

    #finished
    echo "[R&Wbase Install] Done! Now ready to run."
else
    echo "[R&Wbase Install] Aborted. R&Wbase was not installed."
fi