R&Wbase-server
==============

Fuseki-based implementation of a distributed versioned triple store.

Install R&Wbase
----------------
Install the latest version of [OpenLink Virtuoso 6](https://github.com/openlink/virtuoso-opensource) first, or build it from [source](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSMake) 

WARNING: This implementation is NOT compatible with Virtuoso 7 due to its column-store architecture.

Once Virtuoso is installed, make sure it is running.

Next, run the script `install.sh` from `install/`. Be careful, installing R&Wbase will delete all triples present in Virtuoso and change its behaviour.
The install script depends on the environment variable `RAWBASE_HOME` to be set and Virtuoso's `isql` command to be accessible. Both can also be supplied with command parameters.

The following command parameters are optional:
```
    -i The path to the Virtuoso isql application
    -h The RAWBASE_HOME folder where the source is found
    -a The address Virtuoso ODBC is running on, default localhost:1111
    -u The Virtuoso user, default dba
    -p The Virtuoso pass, default dba
```

Run R&Wbase
----------------

After R&Wbase has been installed, run it with the following command:
```
    ./rawbase-server.sh 8080
```
The argument is optional and determines the port of the server. Default 3030