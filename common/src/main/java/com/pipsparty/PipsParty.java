package com.pipsparty;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PipsParty {
    public static final String MOD_ID = "pipsparty";
    public static final Logger LOG = LoggerFactory.getLogger("Pip's Party Palace");

    private PipsParty() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        LOG.info("Pip's Party Palace is open for business... for now.");
    }
}
