package com.gilgamesh.tensura;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
final class GilgameshLog {
    private static final Logger LOG=LoggerFactory.getLogger("GilgameshGoldenGates");
    static void error(String message,Throwable t){LOG.error(message,t);}
}
