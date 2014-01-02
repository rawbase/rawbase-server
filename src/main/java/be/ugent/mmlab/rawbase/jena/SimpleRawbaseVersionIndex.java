/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author mielvandersande
 */
public class SimpleRawbaseVersionIndex implements RawbaseCommitIndex {

    private Map<Integer, Integer> index = new HashMap<>();
    private Map<String, Integer> hashMap = new HashMap<>();

    private boolean isValid(int version) {
        return version % 2 == 0;
    }

    @Override
    public int nextDelta() {
        int max = 0;
        
        if (hashMap.values().isEmpty()) {
            return max;
        }

        for (int key : hashMap.values()) {
            max = key > max ? key : max;
        }

        return max + 2;
    }

    private Integer[] resolveVersion(int version) {
        if (!isValid(version)) {
            return new Integer[0];
        }

        List<Integer> path = new ArrayList<>();

        path.add(0);
        path.add(1);

        int parent = version;

        while (parent > 0) {
            path.add(parent); //2
            path.add(parent + 1);
            parent = index.get(parent); //0
        }

        return path.toArray(new Integer[0]);
    }

    @Override
    public Integer[] resolveCommit(String hash) {
        return resolveVersion(getVersionFromHash(hash));
    }

    private int getVersionFromHash(String hash) {
        return hashMap.get(hash);
    }

    @Override
    public void addCommit(String hash, String parent) {
        int delta = hashMap.get(hash);
        int pDelta = hashMap.get(parent);

        index.put(delta, pDelta);
    }

    @Override
    public void addDeltaMap(String hash, int delta) {
        if (isValid(delta)) {
            hashMap.put(hash, delta);
        }

    }


    @Override
    public Set<String> getCommits() {
        return hashMap.keySet();
    }

    private int getLastVersion() {
        if (hashMap.values().isEmpty()) {
            return -1;
        }
        return Collections.max(hashMap.values());
    }

    @Override
    public Integer[] resolveLastCommit() {
        return resolveVersion(getLastVersion());
    }

    @Override
    public boolean hashExists(String hash) {
        return hashMap.containsKey(hash);
    }
}
