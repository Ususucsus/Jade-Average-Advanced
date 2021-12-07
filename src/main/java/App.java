import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class App {
    private static final List<List<Integer>> graph;

    static {
        //noinspection ArraysAsListWithZeroOrOneArgument
        graph = Arrays.asList(
                Arrays.asList(1, 3),
                Arrays.asList(0, 2, 3),
                Arrays.asList(1, 3),
                Arrays.asList(0, 1, 2, 4),
                Arrays.asList(3)
        );
    }

    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "10098");
        profile.setParameter(Profile.GUI, "true");
        ContainerController mainContainer = runtime.createMainContainer(profile);

        initializeNodes(mainContainer);
    }

    private static void initializeNodes(ContainerController container) {
        ArrayList<Double> nodesValues = new ArrayList<>();

        for (int i = 0; i < graph.size(); i++) {
            List<Integer> nodeNeighbors = graph.get(i);

            String agentNickname = "average-agent-" + i;
            double agentValue = ThreadLocalRandom.current().nextInt(1, 101);
            nodesValues.add(agentValue);

            ArrayList<Object> agentArguments = new ArrayList<>(List.of(agentValue));

            for (int neighborNode : nodeNeighbors) {
                String neighborAgentNickname = "average-agent-" + neighborNode;
                agentArguments.add(neighborAgentNickname);
            }

            try {
                AgentController agent = container.createNewAgent(agentNickname, "AverageAgent", agentArguments.toArray());
                agent.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double nodesValuesSum = 0;

        for (double nodeValue : nodesValues) {
            nodesValuesSum += nodeValue;
        }

        double averageValue = nodesValuesSum / graph.size();

        System.out.println("Agents initialized. Expected average value = " + averageValue);
    }
}