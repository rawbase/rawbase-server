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
package org.apache.jena.fuseki.servlets;

import be.ugent.mmlab.jena.rawbase.ProvenanceVersionIndex;
import be.ugent.mmlab.jena.rawbase.RawbaseQueryExecutionFactory;
import be.ugent.mmlab.jena.rawbase.VersionIndex;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import javax.servlet.http.HttpServletRequest;

public class SPARQL_QueryRawbase extends SPARQL_QueryDataset {
    //MVS: added index for versions

    public SPARQL_QueryRawbase(boolean verbose) {
        super(verbose);
    }

    public SPARQL_QueryRawbase(VersionIndex index) {
        this(false);
    }

    @Override
    protected void validateRequest(HttpServletRequest request) {
    }

    @Override
    protected void validateQuery(HttpActionQuery action, Query query) {
    }

    @Override
    protected QueryExecution createQueryExecution(Query query, Dataset dataset) {

        VersionIndex index;
        index = ProvenanceVersionIndex.getInstance();
       
        //1. Extract the version from the graphs
        String queryString = query.toString(Syntax.syntaxSPARQL);
        String hash = "";
	
        Integer[] vPath;
        
        for (String graph : query.getGraphURIs()) {
            int i = -1;

            if (graph.lastIndexOf("#/") >= 0) {
                i = graph.lastIndexOf("#/") + 1;
            } else if (graph.lastIndexOf("#") >= 0) {
                i = graph.lastIndexOf("#") + 1;
            } else {
                i = graph.lastIndexOf("/") + 1;
            }
            //hash = graph.substring(i);
            hash = graph;
            String newGraph = graph.substring(0, i);
	    System.out.println("Resolving version " + hash + " from graph "+ newGraph);

            queryString = queryString.replaceAll(graph, newGraph);
        }
        query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
        
        if (hash.isEmpty()) {
            vPath = index.resolveLastVersion(); //This is not best solution, make better
        } else {
            try {
		vPath = index.resolveVersion(hash);
            } catch (Exception ex) {
                throw new QueryException("Version hash does not exist");
            }
        }
        
        //2. perform query
        return RawbaseQueryExecutionFactory.create(query, dataset, vPath);
    }
}
