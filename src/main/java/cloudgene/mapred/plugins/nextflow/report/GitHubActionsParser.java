package cloudgene.mapred.plugins.nextflow.report;

import java.io.*;
import java.util.*;

public class GitHubActionsParser {

    public List<Command> parseOutput(InputStream in) throws IOException {
        List<Command> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            Command group = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("::")) {
                    Command command = parseCommand(line);
                    if (command != null) {
                        if (command.getName().equals("group")) {
                            group = command;
                            commands.add(command);
                        } else if (command.getName().equals("endgroup")) {
                            if (group == null) {
                                throw new IOException("Found ::endgroup:: without ::group::");
                            }
                            String type = group.getParameters().get("type");
                            if (type == null) {
                                type = "message";
                            }
                            group.setName(type);
                            group = null;
                        } else {
                            if (group != null) {
                                throw new IOException("No ::endgroup:: found.");
                            }
                            commands.add(command);
                        }
                    }
                }else {
                    if (group != null) {
                        String value = group.getParameters().get("value");
                        if (!value.trim().isEmpty()) {
                            value += "\n";
                        }
                        value += line;
                        group.getParameters().put("value", value);
                    }
                }
            }
        }
        return commands;
    }

    public Command parseCommand(String line) {
        int firstSpace = line.indexOf("::");
        int secondSpace = line.indexOf("::", firstSpace);

        if (firstSpace == -1 || secondSpace == -1) {
            System.out.println("Invalid syntax: " + line);
            return null; // Invalid command format
        }

        String[] commandNameAndValue = line.substring(2).trim().split("::", 2);
        String[] commandNameAndParams = commandNameAndValue[0].split(" ", 2);

        String commandName = commandNameAndParams[0];
        String parameters = "";
        if (commandNameAndParams.length > 1) {
            parameters = commandNameAndParams[1];
        }
        String commandValue =  "";
        if (commandNameAndValue.length > 1) {
            commandValue = commandNameAndValue[1];
        }

        Map<String, String> parameterMap = parseParameters(parameters);
        parameterMap.put("value", commandValue);

        return new Command(commandName.toLowerCase(), parameterMap);
    }

    private Map<String, String> parseParameters(String parameters) {
        Map<String, String> paramMap = new HashMap<>();
        String[] pairs = parameters.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                paramMap.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return paramMap;
    }

    public static class Command {
        private  String name;
        private  Map<String, String> parameters;

        public Command(String name, Map<String, String> parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
