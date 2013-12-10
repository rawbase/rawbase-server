-- This script modifies Virtuoso to support r&wbase. 
-- WARNING: running this script deletes all triples

-- 1. Change table: CREATE EXTRA INTEGER COLUMN
DROP TABLE DB.DBA.RDF_QUAD;

CREATE TABLE DB.DBA.RDF_QUAD (
  G IRI_ID_8,
  S IRI_ID_8,
  P IRI_ID_8,
  O ANY,
  DA INTEGER DEFAULT -1,
  PRIMARY KEY (P, S, O, G, DA)
  )
ALTER INDEX RDF_QUAD ON DB.DBA.RDF_QUAD 
  PARTITION (S INT (0hexffff00));

CREATE DISTINCT NO PRIMARY KEY REF BITMAP INDEX RDF_QUAD_SP 
  ON RDF_QUAD (S, P) 
  PARTITION (S INT (0hexffff00));

CREATE BITMAP INDEX RDF_QUAD_POGS 
  ON RDF_QUAD (P, O, G, S) 
  PARTITION (O VARCHAR (-1, 0hexffff));

CREATE DISTINCT NO PRIMARY KEY REF BITMAP INDEX RDF_QUAD_GS 
  ON RDF_QUAD (G, S) 
  PARTITION (S INT (0hexffff00));

CREATE DISTINCT NO PRIMARY KEY REF INDEX RDF_QUAD_OP 
  ON RDF_QUAD (O, P) 
  PARTITION (O VARCHAR (-1, 0hexffff));

-- Extra optional indexes
/*
CREATE BITMAP INDEX RDF_QUAD_OPGSDA
  ON DB.DBA.RDF_QUAD (O, P, G, S, DA) 
  PARTITION (O VARCHAR (-1, 0hexffff));

CREATE BITMAP INDEX RDF_QUAD_SPOGDA 
  ON RDF_QUAD (S, P, O, G, DA) 
  PARTITION (P VARCHAR (-1, 0hexffff));
*/

-- 2. Add versioned query procedure with subselect and integer

CREATE PROCEDURE versioned_sparql_to_sql_text(IN squery VARCHAR, IN versions VARCHAR)
{
        DECLARE qtxt VARCHAR;
        DECLARE vtxt VARCHAR;
        DECLARE subquery VARCHAR;

        -- assemble the subselect that will prepare the version
        subquery := concat('( SELECT MAX(DA) AS D, S, P, O, G
        FROM DB.DBA.RDF_QUAD x
        WHERE DA in (',versions,')
        GROUP BY P, O, G, S
        HAVING MOD(MAX(DA),2) = 0 )');

        -- calculate the SQL from sparql query
        qtxt := string_output_string(sparql_to_sql_text(squery));
        qtxt := replace(qtxt, 'DB.DBA.RDF_QUAD', subquery);

        return qtxt;
}

-- 3. function for executing SQL

CREATE PROCEDURE exec_versioned_sparql(IN squery VARCHAR, IN versions VARCHAR)
{
        DECLARE qtxt VARCHAR;

        IF (length(versions) = 0)
         return 0;

        qtxt := versioned_sparql_to_sql_text(squery, versions);

        -- executing the sql text

        declare meta, _dt any;
          declare inx integer;
          exec (qtxt, null, null, null, 0, meta, _dt);
          inx := 0;

          exec_result_names (meta[0]);
          while (inx < length (_dt))
            {
              exec_result (_dt[inx]);
              inx := inx + 1;
            }
          return length (_dt);
}

