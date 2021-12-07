import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class AverageAgent extends Agent {
    // Agent value in previous step
    private double prevValue;

    // Agent value used for average calculation
    private double value;

    // Neighbors agents nicknames
    private HashSet<String> neighbors;

    // Current time for the agent
    private int time = 0;

    // List to store values received from neighbors
    private ArrayList<Double> neighborsValues;

    // Max number of iterations
    private static final int maxTime = 100;

    @Override
    protected void setup() {
        System.out.println(getAID().getLocalName() + " agent creating");

        neighbors = new HashSet<>();
        neighborsValues = new ArrayList<>();

        parseArguments(getArguments());

        addBehaviour(new ReceiveValuesBehaviour(time));
        addBehaviour(new SendValuesOneShotBehaviour(time));

        System.out.println(getAID().getLocalName() + " agent created");
    }

    private void parseArguments(Object[] arguments) {
        value = (double) arguments[0];
        System.out.println(getAID().getLocalName() + " got value = " + value);

        for (int i = 1; i < arguments.length; i++) {
            neighbors.add((String) arguments[i]);
        }
        System.out.println(getAID().getLocalName() + " got neighbors = " + neighbors);
    }

    @Override
    protected void takeDown() {
        System.out.println(getAID().getLocalName() + " agent terminating");
        System.out.println(getAID().getLocalName() + " agent terminated");
    }

    private class SendValuesOneShotBehaviour extends OneShotBehaviour {
        private final int time;

        SendValuesOneShotBehaviour(int time) {
            this.time = time;
        }

        @Override
        public void action() {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);

            for (String neighborNickname : neighbors) {
                message.addReceiver(new AID(neighborNickname, AID.ISLOCALNAME));
            }

            message.setConversationId(String.valueOf(time));

            double noisedValue = noiseValue(getValue());
            message.setContent(String.valueOf(noisedValue));

            MaybeFailureMessage maybeFailureMessage = new MaybeFailureMessage(message);
            maybeFailureMessage.send();

            System.out.println(getAID().getLocalName() + " agent sent message with content = " + message.getContent());
        }

        // With 98% returns current value, otherwise previous outdated value.
        private double getValue() {
            Random random = new Random();

            if (time == 0 || random.nextDouble() < 0.98) {
                return value;
            } else {
                return prevValue;
            }
        }


        // Adds random noise to value in range [-1%; 1%]
        private double noiseValue(double value) {
            Random random = new Random();

            double noise = -0.01 * value + (0.01 * value + 0.01 * value) * random.nextDouble();

            return value + noise;
        }

        // Wraps ACLMessage. Sends message in 98% of cases. Otherwise, send empty message
        private class MaybeFailureMessage {
            private final ACLMessage message;

            MaybeFailureMessage(ACLMessage message) {
                this.message = message;
            }

            public void send() {
                Random random = new Random();

                if (random.nextDouble() < 0.98) {
                    myAgent.send(message);
                } else {
                    ACLMessage emptyMessage = new ACLMessage(ACLMessage.INFORM);

                    for (String neighborNickname : neighbors) {
                        emptyMessage.addReceiver(new AID(neighborNickname, AID.ISLOCALNAME));
                    }

                    emptyMessage.setConversationId(String.valueOf(time));

                    emptyMessage.setContent("-1");

                    myAgent.send(emptyMessage);
                }
            }
        }
    }

    private class ReceiveValuesBehaviour extends Behaviour {
        private final int time;

        ReceiveValuesBehaviour(int time) {
            System.out.println(getAID().getLocalName() + " agent starts receiving values at time = " + time);

            this.time = time;
        }

        @Override
        public void action() {
            MessageTemplate messageTemplate = MessageTemplate.MatchConversationId(String.valueOf(time));

            ACLMessage message = myAgent.receive(messageTemplate);

            if (message != null) {
                System.out.println(getAID().getLocalName() + " agent got message with content = " + message.getContent());

                String content = message.getContent();
                double neighbourValue = Double.parseDouble(content);

                neighborsValues.add(neighbourValue);
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            if (neighborsValues.size() == neighbors.size()) {
                System.out.println(getAID().getLocalName() + " agent stops receiving values at time = " + time);

                myAgent.addBehaviour(new CalculateAverageOneShotBehaviour());

                return true;
            }

            return false;
        }
    }

    private class CalculateAverageOneShotBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            double neighborsValuesSum = 0;
            double neighborsSize = 0;

            // ignore empty messages
            for (double neighborValue : neighborsValues) {
                if (neighborValue != -1) {
                    neighborsSize++;
                }
            }

            double alpha = 1.0 / (1.0 + neighborsSize);


            for (double neighborValue : neighborsValues) {
                if (neighborValue != -1) { // ignore empty messages
                    neighborsValuesSum += neighborValue;
                }
            }

            prevValue = value;
            value = alpha * (value + neighborsValuesSum);

            System.out.println(getAID().getLocalName() + " agent recalculated average = " + value);

            time++;
            neighborsValues.clear();

            if (time <= maxTime) {
                myAgent.addBehaviour(new ReceiveValuesBehaviour(time));
                myAgent.addBehaviour(new SendValuesOneShotBehaviour(time));
            } else {
                System.out.println(getAID().getLocalName() + " agent finished with average value = " + value);
            }
        }
    }
}