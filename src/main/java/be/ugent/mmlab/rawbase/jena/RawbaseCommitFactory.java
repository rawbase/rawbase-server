/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena;


import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author mielvandersande
 */
public class RawbaseCommitFactory {
    
    /**
     *
     * @param authorRes
     * @param committerRes
     * @param message
     * @param parent
     * @param persistent
     * @param graph
     * @return
     */
    public static RawbaseCommit create(Resource authorRes, Resource committerRes, String message, String parent, boolean persistent, VirtGraph graph){
        if (parent == null){
            if (persistent){
                return new RawbaseCommitProxy(authorRes, committerRes, message, graph);
            } else {
                return new RawbaseCommitMem(authorRes, committerRes, message);
            }
        } else {
            Resource parentRes = new ResourceImpl(parent);
            if (persistent){
                return new RawbaseCommitProxy(authorRes, committerRes, message, parentRes, graph);
            } else {
                return new RawbaseCommitMem(authorRes, committerRes, message, parentRes);
            }
        }
    }
    
}
