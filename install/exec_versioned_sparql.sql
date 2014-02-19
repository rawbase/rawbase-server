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
