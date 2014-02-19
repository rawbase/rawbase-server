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
