package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import ser.ContactManagement

import org.junit.*
import ser.ContractorManagement
import ser.InvolvePartiesManagement
import ser.InvolvePartiesOnDelete

class ExampleTests {

    Binding binding

    @BeforeClass
    static void initSessionPool() {
        AgentTester.initSessionPool()
    }

    @Before
    void retrieveBinding() {
        binding = AgentTester.retrieveBinding()
    }

    @Test
    void testForAgentResult() {

        //def agent = new InvolvePartiesOnDelete();
        def agent = new InvolvePartiesManagement();

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER24bccad3b4-096b-48c0-8c16-5c18d7e27cd6182024-01-04T14:41:16.898Z011"

        def result = (AgentExecutionResult)agent.execute(binding.variables)
        System.out.println(result)
//        assert result.resultCode == 0
//        assert result.executionMessage.contains("Linux")
//        assert agent.eventInfObj instanceof IDocument
    }

    @Test
    void testForGroovyAgentMethod() {
//        def agent = new GroovyAgent()
//        agent.initializeGroovyBlueline(binding.variables)
//        assert agent.getServerVersion().contains("Linux")
    }

    @Test
    void testForJavaAgentMethod() {
//        def agent = new JavaAgent()
//        agent.initializeGroovyBlueline(binding.variables)
//        assert agent.getServerVersion().contains("Linux")
    }

    @After
    void releaseBinding() {
        println("RLEASE BINDING RUNNING.....")
        AgentTester.releaseBinding(binding)
    }

    @AfterClass
    static void closeSessionPool() {
        AgentTester.closeSessionPool()
    }
}
