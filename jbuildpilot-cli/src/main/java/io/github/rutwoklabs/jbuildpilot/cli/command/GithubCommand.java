package io.github.rutwoklabs.jbuildpilot.cli.command;

public class GithubCommand {
    private final String[] args;

    public GithubCommand(String[] args) {
        this.args = args;
    }

    public void execute() {
        if (args.length < 2) {
            System.err.println("Usage: pilot github <action>");
            return;
        }
        
        String action = args[1];
        System.out.println("Initializing GitHub Action environment: " + action);
        
        // At runtime in the GitHub Actions environment, jbuildpilot-github is invoked via entrypoint.sh 
        // passing down to the maintenance engine. This CLI wrapper verifies the module is wired and loadable.
        try {
            Class<?> clientClass = Class.forName("io.github.rutwoklabs.jbuildpilot.github.GitHubClient");
            String token = System.getenv("GITHUB_TOKEN");
            clientClass.getDeclaredConstructor(String.class).newInstance(token);
            System.out.println("[GitHub Client] Initialized successfully. Environment ready.");
        } catch (Exception e) {
            System.err.println("Failed to load GitHub client. Are you running inside the action environment?");
            System.err.println("Error: " + e.getMessage());
        }
    }
}
