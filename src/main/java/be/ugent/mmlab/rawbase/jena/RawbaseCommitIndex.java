/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena;

import java.util.Set;

/**
 *
 * @author mielvandersande
 */
public interface RawbaseCommitIndex {
    public int nextDelta();
    public void addCommit(String hash, String parent);
    public void addDeltaMap(String hash, int delta);
    
    public Set<String> getCommits();
    
    public Integer[] resolveCommit(String hash);
    public Integer[] resolveLastCommit();
    
    public boolean hashExists(String hash);
}
