/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.ugent.mmlab.rawbase.servlets;

import be.ugent.mmlab.rawbase.jena.RawbaseCommitIndex;
import be.ugent.mmlab.rawbase.jena.RawbaseCommitManager;
import be.ugent.mmlab.rawbase.jena.RawbaseQueryExecutionFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import org.apache.jena.fuseki.servlets.SPARQL_QueryDataset;

public class SPARQL_QueryRawbase extends SPARQL_QueryDataset {

    @Override
    protected QueryExecution createQueryExecution(Query query, Dataset dataset) {

        RawbaseCommitIndex index = RawbaseCommitManager.getInstance().getIndex();
       
        //1. Extract the version from the graph
        String queryString = query.toString(Syntax.syntaxSPARQL);
        String hash = "";
	
        Integer[] vPath;

        for (String graph : query.getGraphURIs()) {
            if (index.hashExists(graph)) {
                hash = graph;
                //Delete FROM that specifies version
                queryString = queryString.replaceAll("FROM\\s*?<" + graph + ">", "");
            }
        }
        query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
        
        if (hash.isEmpty()) {
            //Temp. This needs to be replaced with the current set masterbranch
            vPath = index.resolveLastCommit(); 
        } else {
            try {
                vPath = index.resolveCommit(hash);
            } catch (Exception ex) {
                throw new QueryException("Version hash does not exist");
            }
        }
        
        //2. perform query
        return RawbaseQueryExecutionFactory.create(query, dataset, vPath);
    }
    
    
}
