package caeruleum.maps.utils.managers;

import caeruleum.maps.utils.CaeBasicGenerator;
import caeruleum.maps.utils.CaeChunk;
import caeruleum.maps.utils.CaeMapUtilities;

public class BasicManager {
    protected CaeBasicGenerator gen;
    protected CaeMapUtilities utils;
    public CaeChunk[][] chunks;

    public BasicManager(CaeBasicGenerator gen, CaeMapUtilities utils){
        this.gen = gen;
        this.utils = utils;
    }
}
