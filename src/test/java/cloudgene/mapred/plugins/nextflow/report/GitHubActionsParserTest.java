package cloudgene.mapred.plugins.nextflow.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubActionsParserTest {


    @Test
    public void testParseCommand() {
        GitHubActionsParser parser = new GitHubActionsParser();
        GitHubActionsParser.Command command = parser.parseCommand("::message::this is a message");
        assertEquals("message", command.getName());
        assertEquals("this is a message", command.getParameters().get("value"));
    }

    @Test
    public void testParseCommandWithParam() {
        GitHubActionsParser parser = new GitHubActionsParser();
        GitHubActionsParser.Command command = parser.parseCommand("::message param1=value1, param2=value2::this is a message");
        assertEquals("message", command.getName());
        assertEquals("this is a message", command.getParameters().get("value"));
        assertEquals("value1", command.getParameters().get("param1"));
        assertEquals("value2", command.getParameters().get("param2"));
    }
}