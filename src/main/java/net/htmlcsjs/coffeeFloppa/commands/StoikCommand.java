package net.htmlcsjs.coffeeFloppa.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.*;

public class StoikCommand implements ICommand {
    @Override
    public String getName() {
        return "stoik";
    }

    @Override
    public String execute(Message message) {
        String formulaStr = "";
        try {
            String[] splitFormula = message.getContent().split(" ");
            formulaStr = String.join(" ", Arrays.copyOfRange(splitFormula, 1, splitFormula.length));
        } catch (Exception ignored) {
           return "No formula supplied";
        }

        formulaStr = formulaStr.replace("`", "");

        String[] formulaSides = formulaStr.split("->");
        if (formulaSides.length !=2) {
            return """
                    Error, malformed expression
                    Please format equasion as `Reactant1 + Reactant2 -> Product1 + Product2`
                    E.g. `6CO2 + CH2O -> C6H12O6 + 6O2`
                    """;
        }

        List<String> reactants = new ArrayList<>();
        for (String formula: formulaSides[0].split("\\+")) {
            reactants.addAll(parseChemical(formula));
        }
        List<String> products = new ArrayList<>();
        for (String formula: formulaSides[1].split("\\+")) {
            products.addAll(parseChemical(formula));
        }
        reactants.removeAll(Collections.singleton(""));
        products.removeAll(Collections.singleton(""));

        Map<String, Integer> reactantsMap = new HashMap<>();
        Map<String, Integer> productsMap = new HashMap<>();

        for (String element: reactants) {
            int newVal = reactantsMap.getOrDefault(element, 0) + 1;
            if (newVal != 0) {
                reactantsMap.put(element, newVal);
            }
        }
        for (String element: products) {
            int newVal = productsMap.getOrDefault(element, 0) + 1;
            if (newVal != 0) {
                productsMap.put(element, newVal);
            }
        }

        StringBuilder reactantsStrBuilder = new StringBuilder("```");
        StringBuilder productsStrBuilder = new StringBuilder("```");
        StringBuilder clayStrBuilder = new StringBuilder("```");
        boolean balanced = true;

        List<String> elementKeys = new ArrayList<>();
        elementKeys.addAll(productsMap.keySet());
        elementKeys.addAll(reactantsMap.keySet());

        for (String element: elementKeys.stream().distinct().toList()) {
            if (productsMap.getOrDefault(element, -1) == reactantsMap.getOrDefault(element, -1)) {
                clayStrBuilder.append("✅\u2705");
            } else {
                clayStrBuilder.append("\u274C\u274C");
                balanced = false;
            }
            productsStrBuilder.append(element).append(element.length() == 1 ? ":  " : ": ").append(productsMap.getOrDefault(element, 0)).append("\n");
            reactantsStrBuilder.append(element).append(element.length() == 1 ? ":  " : ": ").append(reactantsMap.getOrDefault(element, 0)).append("\n");

            clayStrBuilder.append("\n");
        }

        clayStrBuilder.append("```");
        reactantsStrBuilder.append("```");
        productsStrBuilder.append("```");
        String balancedMsg;

        if (balanced) {
            balancedMsg = "\u2705 Your reaction is balanced \u2705";
        } else {
            balancedMsg = "\u274C Your reaction is unbalanced \u274C";
        }

        message.getChannel().flatMap(channel -> channel.createMessage().withEmbeds(
                EmbedCreateSpec.builder().addField("Reactants", reactantsStrBuilder.toString(), true)
                        .addField("Products", productsStrBuilder.toString(), true)
                        .addField("Balanced", clayStrBuilder.toString(), true)
                        .addField("Status", balancedMsg, false)
                        .build()
        ).withMessageReference(message.getId())).subscribe();
        return null;

    }

    private List<String> parseChemical(String formula) {
        formula = formula.strip();
        List<String> elements = new ArrayList<>();
        Status status = Status.LOOKING_COMPOUND_MULTIPLIER;
        int bracketLevel = 0;
        StringBuilder count = new StringBuilder();
        StringBuilder element = new StringBuilder();
        String multiplier = "1";

        for (int i = 0; i < formula.length(); i++) {
            String character = formula.split("")[i];
            if (status == Status.NORMAL) {
                if (character.matches("[0-9]")) {
                    count.append(character);
                } else if (character.equals("(")) {
                    bracketLevel++;
                    status = Status.IN_BRACKET;
                    addElements(elements, count, element, multiplier);
                    count = new StringBuilder();
                    element = new StringBuilder();
                } else if (!character.matches("[a-z\\(\\)]")) {
                    addElements(elements, count, element, multiplier);
                    count = new StringBuilder();
                    element = new StringBuilder(character);
                } else if (!character.equals(")")){
                    element.append(character);
                }
            } else if (status == Status.IN_BRACKET) {
                if (character.equals("(")) {
                    bracketLevel++;
                } else if (character.equals(")")) {
                    bracketLevel--;
                    if (bracketLevel == 0) {
                        status = Status.LOOKING_BRACKET_COUNT;
                        continue;
                    }
                }
                element.append(character);
            } else if (status == Status.LOOKING_BRACKET_COUNT) {
                if (character.matches("[0-9]")) {
                    count.append(character);
                } else {
                    status = Status.NORMAL;
                    if (count.isEmpty()) {
                        count.append("1");
                    }
                    List<String> subchemParsed = parseChemical(element.toString());
                    for (long j = 0; j < Long.parseLong(count.toString()) * Long.parseLong(multiplier); j++) {
                        elements.addAll(subchemParsed);
                    }
                    count = new StringBuilder();
                    element = new StringBuilder(character);
                }
            } else if (status == Status.LOOKING_COMPOUND_MULTIPLIER) {
                if (character.matches("[0-9]")) {
                    count.append(character);
                } else if (character.equals("(")) {
                    bracketLevel++;
                    status = Status.IN_BRACKET;
                    count = new StringBuilder();
                    element = new StringBuilder();
                } else {
                    status = Status.NORMAL;
                    if (i != 0) {
                        multiplier = count.toString();
                    }
                    count = new StringBuilder();
                    element.append(character);
                }
            }
        }
        addElements(elements, count, element, multiplier);

        return elements;
    }

    private void addElements(List<String> elements, StringBuilder count, StringBuilder element, String multiplier) {
        if (element.toString().matches("[\\)\\(]")) {
            return;
        }
        if (count.isEmpty()) {
            count.append("1");
        }
        for (long j = 0; j < Long.parseLong(count.toString()) * Long.parseLong(multiplier); j++) {
            elements.add(element.toString());
        }
    }

    private enum Status {
        NORMAL,
        IN_BRACKET,
        LOOKING_BRACKET_COUNT,
        LOOKING_COMPOUND_MULTIPLIER
    }
}

/*                     bracketLevel++;
                    addElements(elements, count, element, multiplier);
                    count = new StringBuilder();
                    element = new StringBuilder();

                    int endPos = 0;
                    for (int j = i; j < formula.length(); j++) {
                        String bracketCharacter = formula.split("")[i];
                        if (bracketCharacter.equals("(")) {
                            bracketLevel++;
                        } else if (bracketCharacter.equals(")")) {
                            bracketLevel--;
                            if (bracketLevel == 0) {
                                endPos = j;
                                break;
                            }
                        }
                    }
                    element.append(formula, i + 1, endPos + 1);
                    i = endPos;*/
