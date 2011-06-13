package it.fridrik.agent;

import java.util.logging.Level;

import it.fridrik.agent.SmithArgs;
import junit.framework.TestCase;

public class TestSmithArgsOld extends TestCase {

    public void testNoParams() {
        String agentargs = null;
        SmithArgs args = new SmithArgs(agentargs);

        assertNull(args.getClassFolders());
        assertNull(args.getJarFolder());
        assertEquals(-1, args.getPeriod());
        assertEquals(Level.WARNING, args.getLogLevel());
        assertFalse(args.isValid());
    }

    public void testParams() {
        String agentargs = " /home/federico/classes ";
        SmithArgs args = new SmithArgs(agentargs);

        assertEquals("/home/federico/classes/", args.getClassFolders());
        assertNull(args.getJarFolder());
        assertEquals(-1, args.getPeriod());
        assertEquals(Level.WARNING, args.getLogLevel());
        assertTrue(args.isValid());
    }

    public void testParamsWithJarFolder() {
        String agentargs = " /home/federico/classes , /home/federico/jars ";
        SmithArgs args = new SmithArgs(agentargs);

        assertEquals("/home/federico/classes/", args.getClassFolders());
        assertEquals("/home/federico/jars/", args.getJarFolder());
        assertEquals(-1, args.getPeriod());
        assertEquals(Level.WARNING, args.getLogLevel());
        assertTrue(args.isValid());
    }

    public void testParamsWithJarFolderAndPeriod() {
        String agentargs = " /home/federico/classes , /home/federico/jars , 599 , SEVERE ";
        SmithArgs args = new SmithArgs(agentargs);

        assertEquals("/home/federico/classes/", args.getClassFolders());
        assertEquals("/home/federico/jars/", args.getJarFolder());
        assertEquals(599, args.getPeriod());
        assertEquals(Level.SEVERE, args.getLogLevel());
        assertTrue(args.isValid());
    }

    public void testToString() {
        String agentargs = " /home/federico/classes , /home/federico/jars , 599 , FINEST ";
        SmithArgs args = new SmithArgs(agentargs);

        assertEquals(
                "classes=/home/federico/classes/,jars=/home/federico/jars/,period=599,loglevel=FINEST",
                args.toString());
    }

}
