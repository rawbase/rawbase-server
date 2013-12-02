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
package be.ugent.mmlab.servlets.rawbase;

import be.ugent.mmlab.jena.rawbase.RawbaseCommitIndex;
import be.ugent.mmlab.jena.rawbase.RawbaseCommitManager;
import be.ugent.mmlab.jena.rawbase.RawbaseQueryExecutionFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.fuseki.servlets.SPARQL_QueryDataset;

public class SPARQL_QueryRawbase extends SPARQL_QueryDataset {
    
    public SPARQL_QueryRawbase(boolean verbose) {
        super(verbose);
    }

    @Override
    protected void validateRequest(HttpServletRequest request) {
        
    }

    @Override
    protected void validateQuery(HttpActionQuery action, Query query) {
        String version = action.request.getParameter("rwb-version");
        if (version != null){
            query.addGraphURI(version);
        }
    }

    
    
    @Override
    protected QueryExecution createQueryExecution(Query query, Dataset dataset) {

        RawbaseCommitIndex index = RawbaseCommitManager.getInstance().getIndex();
       
        //1. Extract the version from the graph
        String queryString = query.toString(Syntax.syntaxSPARQL);
        String hash = "";
        Integer[] vPath;

        for (String graph : query.getGraphURIs()) {
            int i;
            
            //A version hash is identified by being the last part of the graph URI
            //seperated by a # or /
            if (graph.lastIndexOf("#") >= 0) {
                i = graph.lastIndexOf('#') + 1;
            } else {
                i = graph.lastIndexOf('/') + 1;
            }

            hash = graph;
            //Replace graph in FROM clause with real graph
            queryString = queryString.replaceAll("FROM\\s*?<" + graph + ">", "");
            //queryString = queryString.replaceAll(graph, graph.substring(0, i));
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
